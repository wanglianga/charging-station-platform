package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.entity.ChargingOrder;
import com.charging.entity.SettlementRecord;
import com.charging.entity.Station;
import com.charging.mapper.ChargingOrderMapper;
import com.charging.mapper.SettlementRecordMapper;
import com.charging.mapper.StationMapper;
import com.charging.service.SettlementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRecordMapper settlementRecordMapper;
    private final ChargingOrderMapper orderMapper;
    private final StationMapper stationMapper;

    public SettlementServiceImpl(SettlementRecordMapper settlementRecordMapper,
                                 ChargingOrderMapper orderMapper, StationMapper stationMapper) {
        this.settlementRecordMapper = settlementRecordMapper;
        this.orderMapper = orderMapper;
        this.stationMapper = stationMapper;
    }

    @Override
    public Page<SettlementRecord> listSettlements(int page, int size, String status, String period) {
        Page<SettlementRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SettlementRecord::getStatus, status);
        }
        if (period != null && !period.isEmpty()) {
            wrapper.eq(SettlementRecord::getPeriod, period);
        }
        wrapper.orderByDesc(SettlementRecord::getCreatedAt);
        return settlementRecordMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SettlementRecord getSettlementById(Long id) {
        SettlementRecord record = settlementRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("结算记录不存在");
        }
        return record;
    }

    @Override
    @Transactional
    public SettlementRecord calculateSettlement(Long orderId) {
        ChargingOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"COMPLETED".equals(order.getStatus()) && !"INTERRUPTED".equals(order.getStatus())
                && !"FAULT_INTERRUPT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许结算，当前状态: " + order.getStatus());
        }

        LambdaQueryWrapper<SettlementRecord> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(SettlementRecord::getOrderId, orderId);
        SettlementRecord existing = settlementRecordMapper.selectOne(existingWrapper);
        if (existing != null) {
            return existing;
        }

        Station station = stationMapper.selectById(order.getStationId());

        BigDecimal totalElectricityFee = order.getElectricityFee() != null ? order.getElectricityFee() : BigDecimal.ZERO;
        BigDecimal totalServiceFee = order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO;
        BigDecimal totalFee = totalElectricityFee.add(totalServiceFee);

        BigDecimal commissionRate = station != null && station.getCommissionRate() != null
                ? station.getCommissionRate() : BigDecimal.ZERO;
        BigDecimal propertyShareRate = station != null && station.getPropertyShareRate() != null
                ? station.getPropertyShareRate() : BigDecimal.ZERO;

        BigDecimal siteOwnerShare = totalFee.multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal propertyShare = totalFee.multiply(propertyShareRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal operatorShare = totalFee.subtract(siteOwnerShare).subtract(propertyShare);

        String period = order.getEndTime() != null
                ? order.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        SettlementRecord record = new SettlementRecord();
        record.setOrderId(orderId);
        record.setStationId(order.getStationId());
        record.setTotalElectricityFee(totalElectricityFee);
        record.setTotalServiceFee(totalServiceFee);
        record.setOperatorShare(operatorShare);
        record.setSiteOwnerShare(siteOwnerShare);
        record.setPropertyShare(propertyShare);
        record.setStatus("PENDING");
        record.setPeriod(period);
        settlementRecordMapper.insert(record);

        return record;
    }

    @Override
    @Transactional
    public SettlementRecord confirmSettlement(Long id, String status) {
        SettlementRecord record = getSettlementById(id);
        if (!"PENDING".equals(record.getStatus()) && !"DISPUTED".equals(record.getStatus())) {
            throw new RuntimeException("结算记录状态不允许确认，当前状态: " + record.getStatus());
        }

        record.setStatus(status != null ? status : "CONFIRMED");
        record.setConfirmedAt(LocalDateTime.now());
        settlementRecordMapper.updateById(record);
        return record;
    }
}
