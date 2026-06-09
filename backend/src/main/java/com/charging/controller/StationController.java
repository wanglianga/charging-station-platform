package com.charging.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.common.Result;
import com.charging.dto.StationRequest;
import com.charging.entity.Station;
import com.charging.service.StationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public Result<Page<Station>> listStations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return Result.ok(stationService.listStations(page, size, name, status));
    }

    @GetMapping("/{id}")
    public Result<Station> getStation(@PathVariable Long id) {
        return Result.ok(stationService.getStationById(id));
    }

    @PostMapping
    public Result<Station> createStation(@Valid @RequestBody StationRequest request) {
        return Result.ok(stationService.createStation(request));
    }

    @PutMapping("/{id}")
    public Result<Station> updateStation(@PathVariable Long id, @RequestBody StationRequest request) {
        return Result.ok(stationService.updateStation(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return Result.ok();
    }
}
