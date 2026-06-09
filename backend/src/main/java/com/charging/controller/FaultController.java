package com.charging.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.common.Result;
import com.charging.dto.FaultAssignRequest;
import com.charging.dto.FaultResolveRequest;
import com.charging.dto.FaultTicketRequest;
import com.charging.entity.FaultTicket;
import com.charging.service.FaultService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
}
