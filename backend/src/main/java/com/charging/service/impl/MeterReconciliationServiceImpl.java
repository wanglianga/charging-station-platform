package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.*;
import com.charging.entity.*;
import com.charging.mapper.*;
import com.charging.service.MeterReconciliationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeterReconciliationServiceImpl implements MeterReconciliationService {

    private final MeterReconciliationMapper reconciliationMapper;
    private final StationMapper stationMapper;
    private final PileMapper pileMapper;
    private final ChargingOrderMapper orderMapper;
    private final UserMapper userMapper;

    private static final BigDecimal DIFF_THRESHOLD_RATE = new BigDecimal("0.05");

    public MeterReconciliationServiceImpl(MeterReconciliationMapper reconciliationMapper,
                                          StationMapper stationMapper,
                                          PileMapper pileMapper,
                                          ChargingOrderMapper orderMapper,
                                          UserMapper userMapper) {
        this.reconciliationMapper = reconciliationMapper;
        this.stationMapper = stationMapper;
        this.pileMapper = pileMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Page<MeterReconciliation> listReconciliations(int page, int size, String status, Long stationId, String period) {
        Page<MeterReconciliation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MeterReconciliation> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MeterReconciliation::getStatus, status);
        }
        if (stationId != null) {
            wrapper.eq(MeterReconciliation::getStationId, stationId);
        }
        if (period != null && !period.isEmpty()) {
            wrapper.eq(MeterReconciliation::getPeriod, period);
        }
        wrapper.orderByDesc(MeterReconciliation::getCreatedAt);
        return reconciliationMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public MeterReconciliationDetailDTO getReconciliationDetail(Long id) {
        MeterReconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null) {
            throw new RuntimeException("对账记录不存在");
        }

        MeterReconciliationDetailDTO detail = new MeterReconciliationDetailDTO();
        detail.setId(reconciliation.getId());
        detail.setStationId(reconciliation.getStationId());
        detail.setPeriod(reconciliation.getPeriod());
        detail.setStartDate(reconciliation.getStartDate());
        detail.setEndDate(reconciliation.getEndDate());
        detail.setTotalMeterKwh(reconciliation.getTotalMeterKwh());
        detail.setSubMeterTotalKwh(reconciliation.getSubMeterTotalKwh());
        detail.setOrderTotalKwh(reconciliation.getOrderTotalKwh());
        detail.setDifferenceKwh(reconciliation.getDifferenceKwh());
        detail.setDifferenceRate(reconciliation.getDifferenceRate());
        detail.setStatus(reconciliation.getStatus());
        detail.setReviewNote(reconciliation.getReviewNote());
        detail.setLossReason(reconciliation.getLossReason());
        detail.setReviewedBy(reconciliation.getReviewedBy());
        detail.setReviewedAt(reconciliation.getReviewedAt());
        detail.setCreatedAt(reconciliation.getCreatedAt());

        Station station = stationMapper.selectById(reconciliation.getStationId());
        if (station != null) {
            detail.setStationName(station.getName());
        }

        if (reconciliation.getReviewedBy() != null) {
            User reviewer = userMapper.selectById(reconciliation.getReviewedBy());
            if (reviewer != null) {
                detail.setReviewerName(reviewer.getName());
            }
        }

        detail.setPileMeterDetails(calculatePileMeterDetails(
                reconciliation.getStationId(),
                reconciliation.getStartDate(),
                reconciliation.getEndDate()
        ));

        detail.setOrderMeterDetails(calculateOrderMeterDetails(
                reconciliation.getStationId(),
                reconciliation.getStartDate(),
                reconciliation.getEndDate()
        ));

        return detail;
    }

    @Override
    @Transactional
    public MeterReconciliation createReconciliation(MeterReconciliationRequest request) {
        Station station = stationMapper.selectById(request.getStationId());
        if (station == null) {
            throw new RuntimeException("站点不存在");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("开始日期不能晚于结束日期");
        }

        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(LocalTime.MAX);

        LambdaQueryWrapper<ChargingOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(ChargingOrder::getStationId, request.getStationId());
        orderWrapper.ge(ChargingOrder::getEndTime, startDateTime);
        orderWrapper.le(ChargingOrder::getEndTime, endDateTime);
        orderWrapper.in(ChargingOrder::getStatus, "COMPLETED", "INTERRUPTED", "FAULT_INTERRUPT", "OFFLINE_INTERRUPT");
        List<ChargingOrder> orders = orderMapper.selectList(orderWrapper);

        BigDecimal orderTotalKwh = orders.stream()
                .map(o -> o.getChargedKwh() != null ? o.getChargedKwh() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal subMeterTotalKwh = request.getSubMeterTotalKwh() != null
                ? request.getSubMeterTotalKwh()
                : orderTotalKwh;

        BigDecimal differenceKwh = request.getTotalMeterKwh().subtract(orderTotalKwh);
        BigDecimal differenceRate = request.getTotalMeterKwh().compareTo(BigDecimal.ZERO) > 0
                ? differenceKwh.divide(request.getTotalMeterKwh(), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String status = differenceRate.abs().compareTo(DIFF_THRESHOLD_RATE) > 0
                ? "PENDING_REVIEW"
                : "NORMAL";

        String period = request.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        MeterReconciliation reconciliation = new MeterReconciliation();
        reconciliation.setStationId(request.getStationId());
        reconciliation.setPeriod(period);
        reconciliation.setStartDate(request.getStartDate());
        reconciliation.setEndDate(request.getEndDate());
        reconciliation.setTotalMeterKwh(request.getTotalMeterKwh());
        reconciliation.setSubMeterTotalKwh(subMeterTotalKwh);
        reconciliation.setOrderTotalKwh(orderTotalKwh);
        reconciliation.setDifferenceKwh(differenceKwh);
        reconciliation.setDifferenceRate(differenceRate);
        reconciliation.setStatus(status);
        reconciliation.setCreatedAt(LocalDateTime.now());
        reconciliationMapper.insert(reconciliation);

        return reconciliation;
    }

    @Override
    @Transactional
    public MeterReconciliation reviewReconciliation(Long id, Long operatorId, MeterReconciliationReviewRequest request) {
        MeterReconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null) {
            throw new RuntimeException("对账记录不存在");
        }

        if (!"PENDING_REVIEW".equals(reconciliation.getStatus())) {
            throw new RuntimeException("当前状态不允许审核，状态: " + reconciliation.getStatus());
        }

        if (!"CONFIRMED".equals(request.getStatus()) && !"DISPUTED".equals(request.getStatus())) {
            throw new RuntimeException("无效的审核状态: " + request.getStatus());
        }

        reconciliation.setStatus(request.getStatus());
        reconciliation.setReviewNote(request.getReviewNote());
        reconciliation.setLossReason(request.getLossReason());
        reconciliation.setReviewedBy(operatorId);
        reconciliation.setReviewedAt(LocalDateTime.now());
        reconciliation.setUpdatedAt(LocalDateTime.now());
        reconciliationMapper.updateById(reconciliation);

        return reconciliation;
    }

    private List<PileMeterDetailDTO> calculatePileMeterDetails(Long stationId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Pile> pileWrapper = new LambdaQueryWrapper<>();
        pileWrapper.eq(Pile::getStationId, stationId);
        List<Pile> piles = pileMapper.selectList(pileWrapper);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<PileMeterDetailDTO> details = new ArrayList<>();
        for (Pile pile : piles) {
            LambdaQueryWrapper<ChargingOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(ChargingOrder::getPileId, pile.getId());
            orderWrapper.ge(ChargingOrder::getEndTime, startDateTime);
            orderWrapper.le(ChargingOrder::getEndTime, endDateTime);
            orderWrapper.in(ChargingOrder::getStatus, "COMPLETED", "INTERRUPTED", "FAULT_INTERRUPT", "OFFLINE_INTERRUPT");
            List<ChargingOrder> pileOrders = orderMapper.selectList(orderWrapper);

            BigDecimal consumedKwh = pileOrders.stream()
                    .map(o -> o.getChargedKwh() != null ? o.getChargedKwh() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal startKwh = pileOrders.isEmpty() ? BigDecimal.ZERO :
                    pileOrders.stream()
                            .map(ChargingOrder::getStartKwh)
                            .filter(k -> k != null)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

            BigDecimal endKwh = pileOrders.isEmpty() ? BigDecimal.ZERO :
                    pileOrders.stream()
                            .map(ChargingOrder::getEndKwh)
                            .filter(k -> k != null)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

            PileMeterDetailDTO detail = new PileMeterDetailDTO();
            detail.setPileId(pile.getId());
            detail.setPileCode(pile.getPileCode());
            detail.setStartKwh(startKwh);
            detail.setEndKwh(endKwh);
            detail.setConsumedKwh(consumedKwh);
            details.add(detail);
        }
        return details;
    }

    private List<OrderMeterDetailDTO> calculateOrderMeterDetails(Long stationId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<ChargingOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(ChargingOrder::getStationId, stationId);
        orderWrapper.ge(ChargingOrder::getEndTime, startDateTime);
        orderWrapper.le(ChargingOrder::getEndTime, endDateTime);
        orderWrapper.in(ChargingOrder::getStatus, "COMPLETED", "INTERRUPTED", "FAULT_INTERRUPT", "OFFLINE_INTERRUPT");
        orderWrapper.orderByDesc(ChargingOrder::getEndTime);
        List<ChargingOrder> orders = orderMapper.selectList(orderWrapper);

        List<Long> pileIds = orders.stream()
                .map(ChargingOrder::getPileId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Pile> pileWrapper = new LambdaQueryWrapper<>();
        if (!pileIds.isEmpty()) {
            pileWrapper.in(Pile::getId, pileIds);
        }
        List<Pile> piles = pileMapper.selectList(pileWrapper);
        java.util.Map<Long, String> pileCodeMap = piles.stream()
                .collect(Collectors.toMap(Pile::getId, Pile::getPileCode));

        List<OrderMeterDetailDTO> details = new ArrayList<>();
        for (ChargingOrder order : orders) {
            OrderMeterDetailDTO detail = new OrderMeterDetailDTO();
            detail.setOrderId(order.getId());
            detail.setOrderNo(order.getOrderNo());
            detail.setPileId(order.getPileId());
            detail.setPileCode(pileCodeMap.getOrDefault(order.getPileId(), ""));
            detail.setChargedKwh(order.getChargedKwh() != null ? order.getChargedKwh() : BigDecimal.ZERO);
            detail.setStartTime(order.getStartTime());
            detail.setEndTime(order.getEndTime());
            detail.setStatus(order.getStatus());
            details.add(detail);
        }
        return details;
    }
}
