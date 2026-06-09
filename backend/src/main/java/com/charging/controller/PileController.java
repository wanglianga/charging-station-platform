package com.charging.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.common.Result;
import com.charging.dto.PileRequest;
import com.charging.dto.PileStatusUpdateRequest;
import com.charging.entity.Pile;
import com.charging.service.PileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/piles")
public class PileController {

    private final PileService pileService;

    public PileController(PileService pileService) {
        this.pileService = pileService;
    }

    @GetMapping
    public Result<Page<Pile>> listPiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return Result.ok(pileService.listPiles(page, size, status, type));
    }

    @GetMapping("/stations/{stationId}")
    public Result<List<Pile>> listPilesByStation(@PathVariable Long stationId) {
        return Result.ok(pileService.listPilesByStationId(stationId));
    }

    @GetMapping("/{id}")
    public Result<Pile> getPile(@PathVariable Long id) {
        return Result.ok(pileService.getPileById(id));
    }

    @PostMapping
    public Result<Pile> createPile(@Valid @RequestBody PileRequest request) {
        return Result.ok(pileService.createPile(request));
    }

    @PutMapping("/{id}/status")
    public Result<Pile> updatePileStatus(@PathVariable Long id, @Valid @RequestBody PileStatusUpdateRequest request) {
        return Result.ok(pileService.updatePileStatus(id, request.getStatus()));
    }
}
