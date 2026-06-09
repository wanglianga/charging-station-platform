package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.PileRequest;
import com.charging.entity.Pile;
import com.charging.mapper.PileMapper;
import com.charging.service.PileService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PileServiceImpl implements PileService {

    private final PileMapper pileMapper;

    public PileServiceImpl(PileMapper pileMapper) {
        this.pileMapper = pileMapper;
    }

    @Override
    public Page<Pile> listPiles(int page, int size, String status, String type) {
        Page<Pile> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Pile> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Pile::getStatus, status);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Pile::getType, type);
        }
        wrapper.orderByAsc(Pile::getPileCode);
        return pileMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<Pile> listPilesByStationId(Long stationId) {
        LambdaQueryWrapper<Pile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pile::getStationId, stationId).orderByAsc(Pile::getPileCode);
        return pileMapper.selectList(wrapper);
    }

    @Override
    public Pile getPileById(Long id) {
        Pile pile = pileMapper.selectById(id);
        if (pile == null) {
            throw new RuntimeException("充电桩不存在");
        }
        return pile;
    }

    @Override
    public Pile createPile(PileRequest request) {
        Pile pile = new Pile();
        pile.setStationId(request.getStationId());
        pile.setPileCode(request.getPileCode());
        pile.setPower(request.getPower());
        pile.setType(request.getType());
        pile.setStatus("IDLE");
        pileMapper.insert(pile);
        return pile;
    }

    @Override
    public Pile updatePileStatus(Long id, String status) {
        Pile pile = getPileById(id);
        pile.setStatus(status);
        pileMapper.updateById(pile);
        return pile;
    }
}
