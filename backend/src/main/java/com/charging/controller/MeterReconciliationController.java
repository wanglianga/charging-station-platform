package com.charging.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.common.Result;
import com.charging.dto.MeterReconciliationDetailDTO;
import com.charging.dto.MeterReconciliationRequest;
import com.charging.dto.MeterReconciliationReviewRequest;
import com.charging.entity.MeterReconciliation;
import com.charging.service.MeterReconciliationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meter-reconciliations")
public class MeterReconciliationController {

    private final MeterReconciliationService reconciliationService;

    public MeterReconciliationController(MeterReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    public Result<Page<MeterReconciliation>> listReconciliations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) String period) {
        return Result.ok(reconciliationService.listReconciliations(page, size, status, stationId, period));
    }

    @GetMapping("/{id}")
    public Result<MeterReconciliationDetailDTO> getReconciliationDetail(@PathVariable Long id) {
        return Result.ok(reconciliationService.getReconciliationDetail(id));
    }

    @PostMapping
    public Result<MeterReconciliation> createReconciliation(@Valid @RequestBody MeterReconciliationRequest request) {
        return Result.ok(reconciliationService.createReconciliation(request));
    }

    @PutMapping("/{id}/review")
    public Result<MeterReconciliation> reviewReconciliation(
            @PathVariable Long id,
            @RequestParam Long operatorId,
            @Valid @RequestBody MeterReconciliationReviewRequest request) {
        return Result.ok(reconciliationService.reviewReconciliation(id, operatorId, request));
    }
}
