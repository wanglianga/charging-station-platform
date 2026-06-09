package com.charging.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.MeterReadingRequest;
import com.charging.dto.OutageAffectedOrderDTO;
import com.charging.dto.OutageNoticeRequest;
import com.charging.entity.ChargingOrder;
import com.charging.entity.MeterReading;
import com.charging.entity.PowerOutageNotice;
import java.util.List;

public interface SiteOwnerService {

    Page<MeterReading> listMeterReadings(int page, int size, Long stationId);

    MeterReading createMeterReading(MeterReadingRequest request);

    MeterReading confirmMeterReading(Long id, Long userId);

    Page<PowerOutageNotice> listOutageNotices(int page, int size, Long stationId);

    PowerOutageNotice createOutageNotice(Long siteOwnerId, OutageNoticeRequest request);

    List<OutageAffectedOrderDTO> getAffectedOrders(Long stationId, String startTime, String endTime);

    PowerOutageNotice activateOutage(Long noticeId);

    PowerOutageNotice cancelOutage(Long noticeId);

    List<ChargingOrder> settleIncompleteOrders(Long noticeId);
}
