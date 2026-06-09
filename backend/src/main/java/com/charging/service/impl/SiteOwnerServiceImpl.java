package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.MeterReadingRequest;
import com.charging.dto.OutageAffectedOrderDTO;
import com.charging.dto.OutageNoticeRequest;
import com.charging.entity.ChargingOrder;
import com.charging.entity.MeterReading;
import com.charging.entity.Pile;
import com.charging.entity.PowerOutageNotice;
import com.charging.entity.PricingRule;
import com.charging.mapper.ChargingOrderMapper;
import com.charging.mapper.MeterReadingMapper;
import com.charging.mapper.PileMapper;
import com.charging.mapper.PowerOutageNoticeMapper;
import com.charging.mapper.PricingRuleMapper;
import com.charging.service.SettlementService;
import com.charging.service.SiteOwnerService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SiteOwnerServiceImpl implements SiteOwnerService {

    private final MeterReadingMapper meterReadingMapper;
    private final PowerOutageNoticeMapper powerOutageNoticeMapper;
    private final ChargingOrderMapper orderMapper;
    private final PileMapper pileMapper;
    private final PricingRuleMapper pricingRuleMapper;
    private final SettlementService settlementService;

    public SiteOwnerServiceImpl(MeterReadingMapper meterReadingMapper,
                                PowerOutageNoticeMapper powerOutageNoticeMapper,
                                ChargingOrderMapper orderMapper,
                                PileMapper pileMapper,
                                PricingRuleMapper pricingRuleMapper,
                                @Lazy SettlementService settlementService) {
        this.meterReadingMapper = meterReadingMapper;
        this.powerOutageNoticeMapper = powerOutageNoticeMapper;
        this.orderMapper = orderMapper;
        this.pileMapper = pileMapper;
        this.pricingRuleMapper = pricingRuleMapper;
        this.settlementService = settlementService;
    }

    @Override
    public Page<MeterReading> listMeterReadings(int page, int size, Long stationId) {
        Page<MeterReading> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MeterReading> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(MeterReading::getStationId, stationId);
        }
        wrapper.orderByDesc(MeterReading::getReadingDate);
        return meterReadingMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public MeterReading createMeterReading(MeterReadingRequest request) {
        MeterReading reading = new MeterReading();
        reading.setStationId(request.getStationId());
        reading.setReadingType(request.getReadingType());
        reading.setKwh(request.getKwh());
        reading.setReadingDate(request.getReadingDate());
        meterReadingMapper.insert(reading);
        return reading;
    }

    @Override
    public MeterReading confirmMeterReading(Long id, Long userId) {
        MeterReading reading = meterReadingMapper.selectById(id);
        if (reading == null) {
            throw new RuntimeException("抄表记录不存在");
        }
        if (reading.getConfirmedBy() != null) {
            throw new RuntimeException("该抄表记录已确认");
        }
        reading.setConfirmedBy(userId);
        reading.setConfirmedAt(LocalDateTime.now());
        meterReadingMapper.updateById(reading);
        return reading;
    }

    @Override
    public Page<PowerOutageNotice> listOutageNotices(int page, int size, Long stationId) {
        Page<PowerOutageNotice> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PowerOutageNotice> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(PowerOutageNotice::getStationId, stationId);
        }
        wrapper.orderByDesc(PowerOutageNotice::getCreatedAt);
        return powerOutageNoticeMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public PowerOutageNotice createOutageNotice(Long siteOwnerId, OutageNoticeRequest request) {
        PowerOutageNotice notice = new PowerOutageNotice();
        notice.setStationId(request.getStationId());
        notice.setSiteOwnerId(siteOwnerId);
        notice.setStartTime(request.getStartTime());
        notice.setEndTime(request.getEndTime());
        notice.setReason(request.getReason());
        notice.setStatus("SCHEDULED");
        powerOutageNoticeMapper.insert(notice);
        return notice;
    }

    @Override
    public List<OutageAffectedOrderDTO> getAffectedOrders(Long stationId, String startTime, String endTime) {
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);
        LambdaQueryWrapper<ChargingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChargingOrder::getStationId, stationId);
        wrapper.eq(ChargingOrder::getStatus, "CHARGING");
        List<ChargingOrder> orders = orderMapper.selectList(wrapper);

        List<OutageAffectedOrderDTO> result = new ArrayList<>();
        for (ChargingOrder order : orders) {
            OutageAffectedOrderDTO dto = new OutageAffectedOrderDTO();
            dto.setOrderId(order.getId());
            dto.setOrderNo(order.getOrderNo());
            dto.setUserId(order.getUserId());
            dto.setChargedKwh(order.getChargedKwh() != null ? order.getChargedKwh() : BigDecimal.ZERO);
            dto.setTotalFee(order.getTotalFee() != null ? order.getTotalFee() : BigDecimal.ZERO);
            dto.setStatus(order.getStatus());
            result.add(dto);
        }
        return result;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PowerOutageNotice activateOutage(Long noticeId) {
        PowerOutageNotice notice = powerOutageNoticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new RuntimeException("停电通知不存在");
        }
        if (!"SCHEDULED".equals(notice.getStatus())) {
            throw new RuntimeException("只有计划中的通知才能激活，当前状态: " + notice.getStatus());
        }

        notice.setStatus("ACTIVE");

        LambdaQueryWrapper<ChargingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChargingOrder::getStationId, notice.getStationId());
        wrapper.eq(ChargingOrder::getStatus, "CHARGING");
        List<ChargingOrder> affectedOrders = orderMapper.selectList(wrapper);
        notice.setAffectedOrderCount(affectedOrders.size());

        for (ChargingOrder order : affectedOrders) {
            order.setEndTime(LocalDateTime.now());
            order.setInterruptReason("场地方停电: " + notice.getReason());
            order.setOutageNoticeId(noticeId);

            if (order.getEndKwh() != null && order.getStartKwh() != null) {
                BigDecimal charged = order.getEndKwh().subtract(order.getStartKwh());
                order.setChargedKwh(charged.compareTo(BigDecimal.ZERO) > 0 ? charged : BigDecimal.ZERO);
            }

            PricingRule pricingRule = pricingRuleMapper.selectOne(
                    new LambdaQueryWrapper<PricingRule>().eq(PricingRule::getStationId, order.getStationId())
            );
            if (order.getChargedKwh() != null && order.getChargedKwh().compareTo(BigDecimal.ZERO) > 0 && pricingRule != null) {
                BigDecimal pricePerKwh = pricingRule.getFlatPrice();
                BigDecimal electricityFee = order.getChargedKwh().multiply(pricePerKwh).setScale(2, BigDecimal.ROUND_HALF_UP);
                BigDecimal serviceFeeRate = pricingRule.getServiceFeeRate();
                BigDecimal serviceFee = electricityFee.multiply(serviceFeeRate).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                order.setElectricityFee(electricityFee);
                order.setServiceFee(serviceFee);
                order.setTotalFee(electricityFee.add(serviceFee));
            }

            order.setStatus("INTERRUPTED");
            orderMapper.updateById(order);

            Pile pile = pileMapper.selectById(order.getPileId());
            if (pile != null) {
                pile.setStatus("OFFLINE");
                pileMapper.updateById(pile);
            }

            try {
                settlementService.calculateSettlement(order.getId());
            } catch (Exception ignored) {
            }
        }

        powerOutageNoticeMapper.updateById(notice);
        return notice;
    }

    @Override
    public PowerOutageNotice cancelOutage(Long noticeId) {
        PowerOutageNotice notice = powerOutageNoticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new RuntimeException("停电通知不存在");
        }
        if (!"SCHEDULED".equals(notice.getStatus())) {
            throw new RuntimeException("只有计划中的通知才能取消，当前状态: " + notice.getStatus());
        }
        notice.setStatus("CANCELLED");
        powerOutageNoticeMapper.updateById(notice);
        return notice;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public List<ChargingOrder> settleIncompleteOrders(Long noticeId) {
        PowerOutageNotice notice = powerOutageNoticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new RuntimeException("停电通知不存在");
        }

        LambdaQueryWrapper<ChargingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChargingOrder::getStationId, notice.getStationId());
        wrapper.eq(ChargingOrder::getOutageNoticeId, noticeId);
        wrapper.eq(ChargingOrder::getStatus, "INTERRUPTED");
        List<ChargingOrder> orders = orderMapper.selectList(wrapper);

        List<ChargingOrder> settled = new ArrayList<>();
        for (ChargingOrder order : orders) {
            try {
                settlementService.calculateSettlement(order.getId());
                settled.add(order);
            } catch (Exception ignored) {
            }
        }

        notice.setStatus("COMPLETED");
        powerOutageNoticeMapper.updateById(notice);
        return settled;
    }
}
