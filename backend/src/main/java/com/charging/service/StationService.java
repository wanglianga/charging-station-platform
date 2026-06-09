package com.charging.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.StationRequest;
import com.charging.entity.Station;

public interface StationService {

    Page<Station> listStations(int page, int size, String name, String status);

    Station getStationById(Long id);

    Station createStation(StationRequest request);

    Station updateStation(Long id, StationRequest request);

    void deleteStation(Long id);
}
