package com.charging.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.common.Result;
import com.charging.dto.*;
import com.charging.entity.FaultTicket;
import com.charging.service.FaultService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faults")
public class FaultController {

    private final FaultService faultService;

    public FaultController(FaultService faultService) {
        this.faultService = faultService;
    }

    @GetMapping
    public Result<Page<FaultTicket>> listFaults(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String faultType) {
        return Result.ok(faultService.listFaults(page, size, status, faultType));
    }

    @GetMapping("/{id}")
    public Result<FaultTicket> getFault(@PathVariable Long id) {
        return Result.ok(faultService.getFaultById(id));
    }

    @PostMapping
    public Result<FaultTicket> createFaultTicket(@Valid @RequestBody FaultTicketRequest request) {
        return Result.ok(faultService.createFaultTicket(request));
    }

    @PutMapping("/{id}/assign")
    public Result<FaultTicket> assignFault(@PathVariable Long id, @Valid @RequestBody FaultAssignRequest request) {
        return Result.ok(faultService.assignFault(id, request));
    }

    @PutMapping("/{id}/resolve")
    public Result<FaultTicket> resolveFault(@PathVariable Long id, @RequestBody FaultResolveRequest request) {
        return Result.ok(faultService.resolveFault(id, request.getDescription()));
    }

    @PutMapping("/{id}/close")
    public Result<FaultTicket> closeFault(@PathVariable Long id) {
        return Result.ok(faultService.closeFault(id));
    }

    @PostMapping("/dispatch/calculate")
    public Result<RepairDispatchResultDTO> calculateDispatchPriority(@Valid @RequestBody RepairDispatchRequest request) {
        return Result.ok(faultService.calculateDispatchPriority(request));
    }

    @PostMapping("/dispatch/batch")
    public Result<List<RepairDispatchResultDTO>> batchCalculateDispatchPriority(@RequestBody List<Long> faultTicketIds) {
        return Result.ok(faultService.batchCalculateDispatchPriority(faultTicketIds));
    }

    @PostMapping("/accept")
    public Result<FaultTicket> acceptRepair(@Valid @RequestBody RepairAcceptRequest request) {
        return Result.ok(faultService.acceptRepair(request));
    }

    @GetMapping("/{id}/recovery-status")
    public Result<FaultRecoveryStatusDTO> getRecoveryStatus(@PathVariable Long id) {
        return Result.ok(faultService.getRecoveryStatus(id));
    }

    @GetMapping("/station/{stationId}/recovery-status")
    public Result<List<FaultRecoveryStatusDTO>> getStationRecoveryStatus(@PathVariable Long stationId) {
        return Result.ok(faultService.getStationRecoveryStatus(stationId));
    }
}
