package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.OrderCompleteRequest;
import com.charging.dto.OrderCreateRequest;
import com.charging.dto.OrderInterruptRequest;
import com.charging.dto.RefundRequestDTO;
import com.charging.entity.*;
import com.charging.mapper.*;
import com.charging.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl implements OrderService {

    private final ChargingOrderMapper orderMapper;
    private final PileMapper pileMapper;
    private final RefundRequestMapper refundRequestMapper;
    private final PricingRuleMapper pricingRuleMapper;

    public OrderServiceImpl(ChargingOrderMapper orderMapper, PileMapper pileMapper,
                            RefundRequestMapper refundRequestMapper, PricingRuleMapper pricingRuleMapper) {
        this.orderMapper = orderMapper;
        this.pileMapper = pileMapper;
        this.refundRequestMapper = refundRequestMapper;
        this.pricingRuleMapper = pricingRuleMapper;
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
}
