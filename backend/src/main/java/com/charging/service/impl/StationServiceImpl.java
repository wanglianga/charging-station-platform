package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.StationRequest;
import com.charging.entity.Station;
import com.charging.mapper.StationMapper;
import com.charging.service.StationService;
import org.springframework.stereotype.Service;

@Service
public class StationServiceImpl implements StationService {

    private final StationMapper stationMapper;

    public StationServiceImpl(StationMapper stationMapper) {
        this.stationMapper = stationMapper;
    }

    @Override
    public Page<Station> listStations(int page, int size, String name, String status) {
        Page<Station> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Station::getName, name);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Station::getStatus, status);
        }
        wrapper.orderByDesc(Station::getCreatedAt);
        return stationMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Station getStationById(Long id) {
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new RuntimeException("站点不存在");
        }
        return station;
    }

    @Override
    public Station createStation(StationRequest request) {
        Station station = new Station();
        station.setName(request.getName());
        station.setAddress(request.getAddress());
        station.setLongitude(request.getLongitude());
        station.setLatitude(request.getLatitude());
        station.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        station.setSiteOwnerId(request.getSiteOwnerId());
        station.setCommissionRate(request.getCommissionRate() != null ? request.getCommissionRate() : java.math.BigDecimal.ZERO);
        station.setPropertyShareRate(request.getPropertyShareRate() != null ? request.getPropertyShareRate() : java.math.BigDecimal.ZERO);
        stationMapper.insert(station);
        return station;
    }

    @Override
    public Station updateStation(Long id, StationRequest request) {
        Station station = getStationById(id);
        if (request.getName() != null) {
            station.setName(request.getName());
        }
        if (request.getAddress() != null) {
            station.setAddress(request.getAddress());
        }
        if (request.getLongitude() != null) {
            station.setLongitude(request.getLongitude());
        }
        if (request.getLatitude() != null) {
            station.setLatitude(request.getLatitude());
        }
        if (request.getStatus() != null) {
            station.setStatus(request.getStatus());
        }
        if (request.getSiteOwnerId() != null) {
            station.setSiteOwnerId(request.getSiteOwnerId());
        }
        if (request.getCommissionRate() != null) {
            station.setCommissionRate(request.getCommissionRate());
        }
        if (request.getPropertyShareRate() != null) {
            station.setPropertyShareRate(request.getPropertyShareRate());
        }
        stationMapper.updateById(station);
        return station;
    }

    @Override
    public void deleteStation(Long id) {
        getStationById(id);
        stationMapper.deleteById(id);
    }
}
