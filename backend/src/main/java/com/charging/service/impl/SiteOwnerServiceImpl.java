package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.MeterReadingRequest;
import com.charging.dto.OutageNoticeRequest;
import com.charging.entity.MeterReading;
import com.charging.entity.PowerOutageNotice;
import com.charging.mapper.MeterReadingMapper;
import com.charging.mapper.PowerOutageNoticeMapper;
import com.charging.service.SiteOwnerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SiteOwnerServiceImpl implements SiteOwnerService {

    private final MeterReadingMapper meterReadingMapper;
    private final PowerOutageNoticeMapper powerOutageNoticeMapper;

    public SiteOwnerServiceImpl(MeterReadingMapper meterReadingMapper,
                                PowerOutageNoticeMapper powerOutageNoticeMapper) {
        this.meterReadingMapper = meterReadingMapper;
        this.powerOutageNoticeMapper = powerOutageNoticeMapper;
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
}
