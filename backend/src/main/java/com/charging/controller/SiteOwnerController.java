package com.charging.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.common.Result;
import com.charging.dto.MeterReadingRequest;
import com.charging.dto.OutageAffectedOrderDTO;
import com.charging.dto.OutageNoticeRequest;
import com.charging.entity.ChargingOrder;
import com.charging.entity.MeterReading;
import com.charging.entity.PowerOutageNotice;
import com.charging.service.SiteOwnerService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SiteOwnerController {

    private final SiteOwnerService siteOwnerService;

    public SiteOwnerController(SiteOwnerService siteOwnerService) {
        this.siteOwnerService = siteOwnerService;
    }

    @GetMapping("/meter-readings")
    public Result<Page<MeterReading>> listMeterReadings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long stationId) {
        return Result.ok(siteOwnerService.listMeterReadings(page, size, stationId));
    }

    @PostMapping("/meter-readings")
    public Result<MeterReading> createMeterReading(@Valid @RequestBody MeterReadingRequest request) {
        return Result.ok(siteOwnerService.createMeterReading(request));
    }

    @PostMapping("/meter-readings/{id}/confirm")
    public Result<MeterReading> confirmMeterReading(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(siteOwnerService.confirmMeterReading(id, userId));
    }

    @GetMapping("/outage-notices")
    public Result<Page<PowerOutageNotice>> listOutageNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long stationId) {
        return Result.ok(siteOwnerService.listOutageNotices(page, size, stationId));
    }

    @PostMapping("/outage-notices")
    public Result<PowerOutageNotice> createOutageNotice(Authentication authentication,
                                                         @Valid @RequestBody OutageNoticeRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(siteOwnerService.createOutageNotice(userId, request));
    }

    @GetMapping("/outage-notices/{id}/affected-orders")
    public Result<List<OutageAffectedOrderDTO>> getAffectedOrders(
            @PathVariable Long id,
            @RequestParam Long stationId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return Result.ok(siteOwnerService.getAffectedOrders(stationId, startTime, endTime));
    }

    @PostMapping("/outage-notices/{id}/activate")
    public Result<PowerOutageNotice> activateOutage(@PathVariable Long id) {
        return Result.ok(siteOwnerService.activateOutage(id));
    }

    @PutMapping("/outage-notices/{id}/cancel")
    public Result<PowerOutageNotice> cancelOutage(@PathVariable Long id) {
        return Result.ok(siteOwnerService.cancelOutage(id));
    }

    @PostMapping("/outage-notices/{id}/settle")
    public Result<List<ChargingOrder>> settleIncompleteOrders(@PathVariable Long id) {
        return Result.ok(siteOwnerService.settleIncompleteOrders(id));
    }
}
