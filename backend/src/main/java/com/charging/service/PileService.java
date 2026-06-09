package com.charging.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.PileRequest;
import com.charging.entity.Pile;

import java.util.List;

public interface PileService {

    Page<Pile> listPiles(int page, int size, String status, String type);

    List<Pile> listPilesByStationId(Long stationId);

    Pile getPileById(Long id);

    Pile createPile(PileRequest request);

    Pile updatePileStatus(Long id, String status);
}
