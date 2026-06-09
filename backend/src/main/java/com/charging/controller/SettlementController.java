package com.charging.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.common.Result;
import com.charging.dto.SettlementConfirmRequest;
import com.charging.entity.SettlementRecord;
import com.charging.service.SettlementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public Result<Page<SettlementRecord>> listSettlements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String period) {
        return Result.ok(settlementService.listSettlements(page, size, status, period));
    }

    @GetMapping("/{id}")
    public Result<SettlementRecord> getSettlement(@PathVariable Long id) {
        return Result.ok(settlementService.getSettlementById(id));
    }

    @PostMapping
    public Result<SettlementRecord> calculateSettlement(@RequestParam Long orderId) {
        return Result.ok(settlementService.calculateSettlement(orderId));
    }

    @PutMapping("/{id}/confirm")
    public Result<SettlementRecord> confirmSettlement(@PathVariable Long id,
                                                       @RequestBody SettlementConfirmRequest request) {
        return Result.ok(settlementService.confirmSettlement(id, request.getStatus()));
    }
}
