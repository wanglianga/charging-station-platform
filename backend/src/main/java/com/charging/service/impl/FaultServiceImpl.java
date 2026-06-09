package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.FaultAssignRequest;
import com.charging.dto.FaultTicketRequest;
import com.charging.entity.FaultTicket;
import com.charging.entity.Pile;
import com.charging.entity.User;
import com.charging.mapper.FaultTicketMapper;
import com.charging.mapper.PileMapper;
import com.charging.mapper.UserMapper;
import com.charging.service.FaultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FaultServiceImpl implements FaultService {

    private final FaultTicketMapper faultTicketMapper;
    private final PileMapper pileMapper;
    private final UserMapper userMapper;

    public FaultServiceImpl(FaultTicketMapper faultTicketMapper, PileMapper pileMapper, UserMapper userMapper) {
        this.faultTicketMapper = faultTicketMapper;
        this.pileMapper = pileMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Page<FaultTicket> listFaults(int page, int size, String status, String faultType) {
        Page<FaultTicket> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FaultTicket> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(FaultTicket::getStatus, status);
        }
        if (faultType != null && !faultType.isEmpty()) {
            wrapper.eq(FaultTicket::getFaultType, faultType);
        }
        wrapper.orderByDesc(FaultTicket::getCreatedAt);
        return faultTicketMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public FaultTicket getFaultById(Long id) {
        FaultTicket ticket = faultTicketMapper.selectById(id);
        if (ticket == null) {
            throw new RuntimeException("故障工单不存在");
        }
        return ticket;
    }

    @Override
    @Transactional
    public FaultTicket createFaultTicket(FaultTicketRequest request) {
        FaultTicket ticket = new FaultTicket();
        ticket.setStationId(request.getStationId());
        ticket.setPileId(request.getPileId());
        ticket.setFaultType(request.getFaultType());
        ticket.setSeverity(request.getSeverity() != null ? request.getSeverity() : "MEDIUM");
        ticket.setStatus("PENDING");
        ticket.setDescription(request.getDescription());
        faultTicketMapper.insert(ticket);

        Pile pile = pileMapper.selectById(request.getPileId());
        if (pile != null) {
            pile.setStatus("FAULT");
            pileMapper.updateById(pile);
        }

        return ticket;
    }

    @Override
    @Transactional
    public FaultTicket assignFault(Long id, FaultAssignRequest request) {
        FaultTicket ticket = getFaultById(id);
        if (!"PENDING".equals(ticket.getStatus())) {
            throw new RuntimeException("工单状态不允许派单，当前状态: " + ticket.getStatus());
        }

        User engineer = userMapper.selectById(request.getEngineerId());
        if (engineer == null || !"ENGINEER".equals(engineer.getRole())) {
            throw new RuntimeException("指定的运维工程师不存在或角色不匹配");
        }

        ticket.setAssignedEngineerId(request.getEngineerId());
        ticket.setStatus("ASSIGNED");
        ticket.setAssignedAt(LocalDateTime.now());
        faultTicketMapper.updateById(ticket);
        return ticket;
    }

    @Override
    @Transactional
    public FaultTicket resolveFault(Long id, String description) {
        FaultTicket ticket = getFaultById(id);
        if (!"ASSIGNED".equals(ticket.getStatus()) && !"PROCESSING".equals(ticket.getStatus())) {
            throw new RuntimeException("工单状态不允许处理，当前状态: " + ticket.getStatus());
        }

        ticket.setStatus("RESOLVED");
        ticket.setResolvedAt(LocalDateTime.now());
        if (description != null && !description.isEmpty()) {
            String existingDesc = ticket.getDescription() != null ? ticket.getDescription() : "";
            ticket.setDescription(existingDesc + "\n[处理结果]: " + description);
        }
        faultTicketMapper.updateById(ticket);

        Pile pile = pileMapper.selectById(ticket.getPileId());
        if (pile != null && "FAULT".equals(pile.getStatus())) {
            pile.setStatus("IDLE");
            pileMapper.updateById(pile);
        }

        return ticket;
    }

    @Override
    @Transactional
    public FaultTicket closeFault(Long id) {
        FaultTicket ticket = getFaultById(id);
        if (!"RESOLVED".equals(ticket.getStatus())) {
            throw new RuntimeException("工单状态不允许关闭，当前状态: " + ticket.getStatus());
        }

        ticket.setStatus("CLOSED");
        faultTicketMapper.updateById(ticket);
        return ticket;
    }
}
