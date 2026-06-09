package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.OrderCompleteRequest;
import com.charging.dto.OrderCreateRequest;
import com.charging.dto.OrderInterruptRequest;
import com.charging.dto.RefundRequestDTO;
import com.charging.dto.CompensationDecisionRequest;
import com.charging.dto.InterruptionDetailDTO;
import com.charging.dto.SwitchablePileDTO;
import com.charging.entity.*;
import com.charging.mapper.*;
import com.charging.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final ChargingOrderMapper orderMapper;
    private final PileMapper pileMapper;
    private final RefundRequestMapper refundRequestMapper;
    private final PricingRuleMapper pricingRuleMapper;
    private final InterruptionCompensationMapper compensationMapper;

    public OrderServiceImpl(ChargingOrderMapper orderMapper, PileMapper pileMapper,
                            RefundRequestMapper refundRequestMapper, PricingRuleMapper pricingRuleMapper,
                            InterruptionCompensationMapper compensationMapper) {
        this.orderMapper = orderMapper;
        this.pileMapper = pileMapper;
        this.refundRequestMapper = refundRequestMapper;
        this.pricingRuleMapper = pricingRuleMapper;
        this.compensationMapper = compensationMapper;
    }

    @Override
    public Page<ChargingOrder> listOrders(int page, int size, String status, Long userId) {
        Page<ChargingOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChargingOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ChargingOrder::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(ChargingOrder::getUserId, userId);
        }
        wrapper.orderByDesc(ChargingOrder::getCreatedAt);
        return orderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public ChargingOrder getOrderById(Long id) {
        ChargingOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        return order;
    }

    @Override
    @Transactional
    public ChargingOrder createOrder(Long userId, OrderCreateRequest request) {
        Pile pile = pileMapper.selectById(request.getPileId());
        if (pile == null) {
            throw new RuntimeException("充电桩不存在");
        }
        if (!"IDLE".equals(pile.getStatus())) {
            throw new RuntimeException("充电桩当前不可用，状态: " + pile.getStatus());
        }

        ChargingOrder order = new ChargingOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPileId(pile.getId());
        order.setStationId(pile.getStationId());
        order.setStatus("PENDING");
        order.setStartKwh(BigDecimal.ZERO);
        order.setEndKwh(BigDecimal.ZERO);
        order.setChargedKwh(BigDecimal.ZERO);
        order.setElectricityFee(BigDecimal.ZERO);
        order.setServiceFee(BigDecimal.ZERO);
        order.setTotalFee(BigDecimal.ZERO);
        orderMapper.insert(order);

        pile.setStatus("CHARGING");
        pileMapper.updateById(pile);

        return order;
    }

    @Override
    @Transactional
    public ChargingOrder startCharging(Long orderId) {
        ChargingOrder order = getOrderById(orderId);
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许启动充电，当前状态: " + order.getStatus());
        }
        order.setStatus("CHARGING");
        order.setStartTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    @Override
    @Transactional
    public ChargingOrder completeCharging(Long orderId, OrderCompleteRequest request) {
        ChargingOrder order = getOrderById(orderId);
        if (!"CHARGING".equals(order.getStatus()) && !"HANDSHAKE".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许完成充电，当前状态: " + order.getStatus());
        }

        BigDecimal endKwh = request.getEndKwh() != null ? request.getEndKwh() : BigDecimal.ZERO;
        order.setEndKwh(endKwh);
        order.setChargedKwh(endKwh.subtract(order.getStartKwh()));
        order.setEndTime(LocalDateTime.now());

        calculateFees(order);

        order.setStatus("COMPLETED");
        orderMapper.updateById(order);

        Pile pile = pileMapper.selectById(order.getPileId());
        if (pile != null) {
            pile.setStatus("IDLE");
            pileMapper.updateById(pile);
        }

        return order;
    }

    @Override
    @Transactional
    public ChargingOrder interruptCharging(Long orderId, OrderInterruptRequest request) {
        ChargingOrder order = getOrderById(orderId);
        if (!"CHARGING".equals(order.getStatus()) && !"HANDSHAKE".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许中断充电，当前状态: " + order.getStatus());
        }

        order.setEndTime(LocalDateTime.now());
        order.setInterruptReason(request.getReason() != null ? request.getReason() : "手动中断");

        if (order.getEndKwh() != null && order.getStartKwh() != null) {
            BigDecimal charged = order.getEndKwh().subtract(order.getStartKwh());
            order.setChargedKwh(charged.compareTo(BigDecimal.ZERO) > 0 ? charged : BigDecimal.ZERO);
        }

        calculateFees(order);

        order.setStatus("INTERRUPTED");
        orderMapper.updateById(order);

        Pile pile = pileMapper.selectById(order.getPileId());
        if (pile != null) {
            pile.setStatus("IDLE");
            pileMapper.updateById(pile);
        }

        return order;
    }

    @Override
    @Transactional
    public RefundRequest requestRefund(Long userId, RefundRequestDTO request) {
        ChargingOrder order = getOrderById(request.getOrderId());
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("只能为自己的订单申请退款");
        }
        if (!"COMPLETED".equals(order.getStatus()) && !"INTERRUPTED".equals(order.getStatus())
                && !"FAULT_INTERRUPT".equals(order.getStatus()) && !"OFFLINE_INTERRUPT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许退款，当前状态: " + order.getStatus());
        }

        RefundRequest refund = new RefundRequest();
        refund.setOrderId(order.getId());
        refund.setUserId(userId);
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());
        refund.setStatus("PENDING");
        refundRequestMapper.insert(refund);

        order.setStatus("REFUNDING");
        orderMapper.updateById(order);

        return refund;
    }

    private void calculateFees(ChargingOrder order) {
        if (order.getChargedKwh() == null || order.getChargedKwh().compareTo(BigDecimal.ZERO) <= 0) {
            order.setElectricityFee(BigDecimal.ZERO);
            order.setServiceFee(BigDecimal.ZERO);
            order.setTotalFee(BigDecimal.ZERO);
            return;
        }

        PricingRule pricingRule = pricingRuleMapper.selectOne(
                new LambdaQueryWrapper<PricingRule>().eq(PricingRule::getStationId, order.getStationId())
        );

        BigDecimal pricePerKwh;
        if (pricingRule != null) {
            pricePerKwh = pricingRule.getFlatPrice();
        } else {
            pricePerKwh = new BigDecimal("0.80");
        }

        BigDecimal electricityFee = order.getChargedKwh().multiply(pricePerKwh).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal serviceFeeRate = pricingRule != null ? pricingRule.getServiceFeeRate() : BigDecimal.ZERO;
        BigDecimal serviceFee = electricityFee.multiply(serviceFeeRate).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);

        order.setElectricityFee(electricityFee);
        order.setServiceFee(serviceFee);
        order.setTotalFee(electricityFee.add(serviceFee));
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + timestamp + random;
    }

    @Override
    public InterruptionDetailDTO getInterruptionDetail(Long orderId) {
        ChargingOrder order = getOrderById(orderId);
        LambdaQueryWrapper<InterruptionCompensation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterruptionCompensation::getOrderId, orderId);
        InterruptionCompensation comp = compensationMapper.selectOne(wrapper);

        InterruptionDetailDTO detail = new InterruptionDetailDTO();
        detail.setOrderId(orderId);
        detail.setOrderNo(order.getOrderNo());
        detail.setChargedKwh(order.getChargedKwh() != null ? order.getChargedKwh() : BigDecimal.ZERO);
        detail.setElectricityFee(order.getElectricityFee() != null ? order.getElectricityFee() : BigDecimal.ZERO);
        detail.setServiceFee(order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO);
        detail.setTotalFee(order.getTotalFee() != null ? order.getTotalFee() : BigDecimal.ZERO);
        detail.setStopReason(order.getInterruptReason() != null ? order.getInterruptReason() : "未知原因");

        if (comp != null) {
            detail.setCompensationId(comp.getId());
            detail.setWaitingMinutes(comp.getWaitingMinutes());
            detail.setDecision(comp.getDecision());
            detail.setSwitchablePiles(resolveSwitchablePiles(comp.getSwitchablePiles()));
        } else {
            detail.setWaitingMinutes(0);
            detail.setDecision("PENDING");
            LambdaQueryWrapper<Pile> idleWrapper = new LambdaQueryWrapper<>();
            idleWrapper.eq(Pile::getStationId, order.getStationId());
            idleWrapper.eq(Pile::getStatus, "IDLE");
            List<Pile> idlePiles = pileMapper.selectList(idleWrapper);
            detail.setSwitchablePiles(idlePiles.stream().map(this::toSwitchablePileDTO).collect(Collectors.toList()));
        }

        return detail;
    }

    @Override
    @Transactional
    public ChargingOrder handleCompensationDecision(Long userId, CompensationDecisionRequest request) {
        InterruptionCompensation comp = compensationMapper.selectById(request.getCompensationId());
        if (comp == null) {
            throw new RuntimeException("补偿记录不存在");
        }
        if (!comp.getUserId().equals(userId)) {
            throw new RuntimeException("只能处理自己的补偿记录");
        }
        if (!"PENDING".equals(comp.getDecision())) {
            throw new RuntimeException("该补偿记录已处理，当前决定: " + comp.getDecision());
        }

        ChargingOrder originalOrder = getOrderById(comp.getOrderId());

        switch (request.getDecision()) {
            case "CONTINUE":
                comp.setDecision("CONTINUE");
                comp.setDecidedAt(LocalDateTime.now());
                compensationMapper.updateById(comp);
                originalOrder.setStatus("CHARGING");
                originalOrder.setEndTime(null);
                orderMapper.updateById(originalOrder);
                Pile origPile = pileMapper.selectById(originalOrder.getPileId());
                if (origPile != null) {
                    origPile.setStatus("CHARGING");
                    pileMapper.updateById(origPile);
                }
                return originalOrder;

            case "REFUND":
                comp.setDecision("REFUND");
                comp.setDecidedAt(LocalDateTime.now());
                compensationMapper.updateById(comp);
                RefundRequest refund = new RefundRequest();
                refund.setOrderId(originalOrder.getId());
                refund.setUserId(userId);
                refund.setAmount(originalOrder.getTotalFee());
                refund.setReason("充电中断补偿退款: " + comp.getStopReason());
                refund.setStatus("PENDING");
                refundRequestMapper.insert(refund);
                originalOrder.setStatus("REFUNDING");
                orderMapper.updateById(originalOrder);
                return originalOrder;

            case "SWITCH":
                if (request.getSwitchTargetPileId() == null) {
                    throw new RuntimeException("换桩必须指定目标桩位");
                }
                Pile targetPile = pileMapper.selectById(request.getSwitchTargetPileId());
                if (targetPile == null || !"IDLE".equals(targetPile.getStatus())) {
                    throw new RuntimeException("目标桩位不可用");
                }
                comp.setDecision("SWITCH");
                comp.setSwitchTargetPileId(request.getSwitchTargetPileId());
                comp.setDecidedAt(LocalDateTime.now());
                compensationMapper.updateById(comp);

                ChargingOrder newOrder = new ChargingOrder();
                newOrder.setOrderNo(generateOrderNo());
                newOrder.setUserId(userId);
                newOrder.setPileId(targetPile.getId());
                newOrder.setStationId(targetPile.getStationId());
                newOrder.setStatus("PENDING");
                newOrder.setStartKwh(BigDecimal.ZERO);
                newOrder.setEndKwh(BigDecimal.ZERO);
                newOrder.setChargedKwh(BigDecimal.ZERO);
                newOrder.setElectricityFee(BigDecimal.ZERO);
                newOrder.setServiceFee(BigDecimal.ZERO);
                newOrder.setTotalFee(BigDecimal.ZERO);
                newOrder.setLinkedOrderId(originalOrder.getId());
                orderMapper.insert(newOrder);
                comp.setSwitchOrderId(newOrder.getId());
                compensationMapper.updateById(comp);

                originalOrder.setLinkedOrderId(newOrder.getId());
                orderMapper.updateById(originalOrder);

                targetPile.setStatus("CHARGING");
                pileMapper.updateById(targetPile);
                return newOrder;

            default:
                throw new RuntimeException("无效的决定: " + request.getDecision());
        }
    }

    private List<SwitchablePileDTO> resolveSwitchablePiles(String switchablePilesStr) {
        if (switchablePilesStr == null || switchablePilesStr.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> pileIds = Arrays.stream(switchablePilesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        List<SwitchablePileDTO> result = new ArrayList<>();
        for (Long pileId : pileIds) {
            Pile pile = pileMapper.selectById(pileId);
            if (pile != null) {
                result.add(toSwitchablePileDTO(pile));
            }
        }
        return result;
    }

    private SwitchablePileDTO toSwitchablePileDTO(Pile pile) {
        SwitchablePileDTO dto = new SwitchablePileDTO();
        dto.setPileId(pile.getId());
        dto.setPileCode(pile.getPileCode());
        dto.setPower(pile.getPower());
        dto.setType(pile.getType());
        dto.setStatus(pile.getStatus());
        return dto;
    }
}
