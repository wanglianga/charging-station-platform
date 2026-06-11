package com.charging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.*;
import com.charging.entity.*;
import com.charging.mapper.*;
import com.charging.service.FaultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class FaultServiceImpl implements FaultService {

    private final FaultTicketMapper faultTicketMapper;
    private final PileMapper pileMapper;
    private final UserMapper userMapper;
    private final StationMapper stationMapper;
    private final ChargingOrderMapper orderMapper;

    private static final int DEFAULT_REPAIR_MINUTES = 60;
    private static final BigDecimal WEIGHT_FAULT_PILES = new BigDecimal("0.30");
    private static final BigDecimal WEIGHT_WAITING_OWNERS = new BigDecimal("0.25");
    private static final BigDecimal WEIGHT_STATION_REVENUE = new BigDecimal("0.20");
    private static final BigDecimal WEIGHT_SITE_OWNER = new BigDecimal("0.15");
    private static final BigDecimal WEIGHT_PARTS_DISTANCE = new BigDecimal("0.10");

    public FaultServiceImpl(FaultTicketMapper faultTicketMapper, PileMapper pileMapper,
                            UserMapper userMapper, StationMapper stationMapper,
                            ChargingOrderMapper orderMapper) {
        this.faultTicketMapper = faultTicketMapper;
        this.pileMapper = pileMapper;
        this.userMapper = userMapper;
        this.stationMapper = stationMapper;
        this.orderMapper = orderMapper;
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
        wrapper.orderByDesc(FaultTicket::getPriorityScore);
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
        ticket.setCreatedAt(LocalDateTime.now());

        int faultPileCount = countStationFaultPiles(request.getStationId());
        ticket.setFaultPileCount(faultPileCount);

        int waitingOwners = countWaitingOwners(request.getStationId());
        ticket.setWaitingCarOwners(waitingOwners);

        ticket.setStationRevenue(calculateStationMonthlyRevenue(request.getStationId()));
        ticket.setPartsDistance(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.5, 15.0)));

        ticket.setPriorityScore(calculatePriorityScore(ticket));
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
        ticket.setEstimatedRestoreTime(LocalDateTime.now().plusMinutes(DEFAULT_REPAIR_MINUTES));
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

    @Override
    @Transactional
    public RepairDispatchResultDTO calculateDispatchPriority(RepairDispatchRequest request) {
        FaultTicket ticket = getFaultById(request.getFaultTicketId());

        if (request.getFaultPileCount() != null) {
            ticket.setFaultPileCount(request.getFaultPileCount());
        }
        if (request.getWaitingCarOwners() != null) {
            ticket.setWaitingCarOwners(request.getWaitingCarOwners());
        }
        if (request.getStationRevenue() != null) {
            ticket.setStationRevenue(request.getStationRevenue());
        }
        if (request.getSiteOwnerRequest() != null) {
            ticket.setSiteOwnerRequest(request.getSiteOwnerRequest());
        }
        if (request.getPartsDistance() != null) {
            ticket.setPartsDistance(request.getPartsDistance());
        }

        ticket.setPriorityScore(calculatePriorityScore(ticket));

        List<EngineerRecommendationDTO> recommendations = recommendEngineers(ticket);
        if (!recommendations.isEmpty()) {
            EngineerRecommendationDTO best = recommendations.get(0);
            ticket.setRecommendedEngineerId(best.getEngineerId());
            ticket.setEstimatedRestoreTime(best.getEstimatedRestoreTime());
            ticket.setRecommendedRoute(generateRouteDescription(ticket, best));
        }

        faultTicketMapper.updateById(ticket);

        return buildDispatchResult(ticket, recommendations);
    }

    @Override
    public List<RepairDispatchResultDTO> batchCalculateDispatchPriority(List<Long> faultTicketIds) {
        List<RepairDispatchResultDTO> results = new ArrayList<>();
        for (Long ticketId : faultTicketIds) {
            RepairDispatchRequest request = new RepairDispatchRequest();
            request.setFaultTicketId(ticketId);
            results.add(calculateDispatchPriority(request));
        }
        results.sort(Comparator.comparing(
                RepairDispatchResultDTO::getPriorityScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return results;
    }

    @Override
    @Transactional
    public FaultTicket acceptRepair(RepairAcceptRequest request) {
        FaultTicket ticket = getFaultById(request.getFaultTicketId());
        if (!"PENDING".equals(ticket.getStatus()) && !"ASSIGNED".equals(ticket.getStatus())) {
            throw new RuntimeException("工单状态不允许接单，当前状态: " + ticket.getStatus());
        }

        Long engineerId = request.getEngineerId() != null ? request.getEngineerId() : ticket.getRecommendedEngineerId();
        if (engineerId == null) {
            throw new RuntimeException("必须指定工程师ID");
        }

        User engineer = userMapper.selectById(engineerId);
        if (engineer == null || !"ENGINEER".equals(engineer.getRole())) {
            throw new RuntimeException("指定的运维工程师不存在或角色不匹配");
        }

        int repairMinutes = request.getEstimatedRepairMinutes() != null
                ? request.getEstimatedRepairMinutes()
                : DEFAULT_REPAIR_MINUTES;

        ticket.setAssignedEngineerId(engineerId);
        ticket.setStatus("PROCESSING");
        ticket.setAssignedAt(LocalDateTime.now());
        ticket.setEstimatedRestoreTime(LocalDateTime.now().plusMinutes(repairMinutes));
        faultTicketMapper.updateById(ticket);

        return ticket;
    }

    @Override
    public FaultRecoveryStatusDTO getRecoveryStatus(Long faultTicketId) {
        FaultTicket ticket = getFaultById(faultTicketId);
        return buildRecoveryStatus(ticket);
    }

    @Override
    public List<FaultRecoveryStatusDTO> getStationRecoveryStatus(Long stationId) {
        LambdaQueryWrapper<FaultTicket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultTicket::getStationId, stationId);
        wrapper.in(FaultTicket::getStatus, "PENDING", "ASSIGNED", "PROCESSING");
        wrapper.orderByDesc(FaultTicket::getPriorityScore);
        List<FaultTicket> tickets = faultTicketMapper.selectList(wrapper);
        return tickets.stream().map(this::buildRecoveryStatus).collect(Collectors.toList());
    }

    private BigDecimal calculatePriorityScore(FaultTicket ticket) {
        BigDecimal faultPileScore = normalizeFaultPiles(ticket.getFaultPileCount());
        BigDecimal waitingScore = normalizeWaitingOwners(ticket.getWaitingCarOwners());
        BigDecimal revenueScore = normalizeRevenue(ticket.getStationRevenue());
        BigDecimal siteOwnerScore = normalizeSiteOwnerRequest(ticket.getSiteOwnerRequest());
        BigDecimal partsScore = normalizePartsDistance(ticket.getPartsDistance());

        return faultPileScore.multiply(WEIGHT_FAULT_PILES)
                .add(waitingScore.multiply(WEIGHT_WAITING_OWNERS))
                .add(revenueScore.multiply(WEIGHT_STATION_REVENUE))
                .add(siteOwnerScore.multiply(WEIGHT_SITE_OWNER))
                .add(partsScore.multiply(WEIGHT_PARTS_DISTANCE))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeFaultPiles(Integer count) {
        if (count == null || count <= 0) return BigDecimal.ZERO;
        int maxPiles = 20;
        return BigDecimal.valueOf(Math.min(count, maxPiles))
                .divide(BigDecimal.valueOf(maxPiles), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal normalizeWaitingOwners(Integer count) {
        if (count == null || count <= 0) return BigDecimal.ZERO;
        int maxOwners = 50;
        return BigDecimal.valueOf(Math.min(count, maxOwners))
                .divide(BigDecimal.valueOf(maxOwners), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal normalizeRevenue(BigDecimal revenue) {
        if (revenue == null || revenue.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal maxRevenue = new BigDecimal("100000");
        return revenue.min(maxRevenue)
                .divide(maxRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal normalizeSiteOwnerRequest(String request) {
        if (request == null) return new BigDecimal("30");
        switch (request.toUpperCase()) {
            case "URGENT":
                return new BigDecimal("100");
            case "HIGH":
                return new BigDecimal("70");
            case "NORMAL":
                return new BigDecimal("30");
            default:
                return new BigDecimal("30");
        }
    }

    private BigDecimal normalizePartsDistance(BigDecimal distance) {
        if (distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) return new BigDecimal("100");
        BigDecimal maxDistance = new BigDecimal("30");
        BigDecimal normalized = BigDecimal.ONE.subtract(
                distance.min(maxDistance).divide(maxDistance, 4, RoundingMode.HALF_UP)
        );
        return normalized.multiply(new BigDecimal("100"));
    }

    private List<EngineerRecommendationDTO> recommendEngineers(FaultTicket ticket) {
        LambdaQueryWrapper<User> engineerWrapper = new LambdaQueryWrapper<>();
        engineerWrapper.eq(User::getRole, "ENGINEER");
        List<User> engineers = userMapper.selectList(engineerWrapper);

        Station station = stationMapper.selectById(ticket.getStationId());

        List<EngineerRecommendationDTO> recommendations = new ArrayList<>();
        for (User engineer : engineers) {
            EngineerRecommendationDTO dto = new EngineerRecommendationDTO();
            dto.setEngineerId(engineer.getId());
            dto.setEngineerName(engineer.getName());
            dto.setEngineerPhone(engineer.getPhone());

            BigDecimal distance = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(1.0, 25.0));
            dto.setDistance(distance);

            int travelMinutes = distance.multiply(new BigDecimal("3")).intValue();
            int repairMinutes = estimateRepairMinutes(ticket.getFaultType());
            int totalMinutes = travelMinutes + repairMinutes;
            dto.setEstimatedArrivalMinutes(travelMinutes);
            dto.setEstimatedArrivalTime(LocalDateTime.now().plusMinutes(travelMinutes));
            dto.setEstimatedRestoreTime(LocalDateTime.now().plusMinutes(totalMinutes));

            dto.setCurrentWorkload(countEngineerActiveTickets(engineer.getId()));

            BigDecimal workloadScore = BigDecimal.valueOf(Math.max(0, 10 - dto.getCurrentWorkload()))
                    .multiply(new BigDecimal("10"));
            BigDecimal distanceScore = normalizePartsDistance(distance);
            BigDecimal matchScore = workloadScore.multiply(new BigDecimal("0.4"))
                    .add(distanceScore.multiply(new BigDecimal("0.6")));
            dto.setMatchScore(matchScore.setScale(2, RoundingMode.HALF_UP));

            recommendations.add(dto);
        }

        recommendations.sort(Comparator.comparing(
                EngineerRecommendationDTO::getMatchScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return recommendations;
    }

    private int estimateRepairMinutes(String faultType) {
        if (faultType == null) return DEFAULT_REPAIR_MINUTES;
        switch (faultType) {
            case "GUN_LINE_FAULT":
                return 45;
            case "MODULE_OVER_TEMP":
                return 30;
            case "COMM_OFFLINE":
                return 20;
            case "EMERGENCY_STOP":
                return 15;
            default:
                return DEFAULT_REPAIR_MINUTES;
        }
    }

    private int countEngineerActiveTickets(Long engineerId) {
        LambdaQueryWrapper<FaultTicket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultTicket::getAssignedEngineerId, engineerId);
        wrapper.in(FaultTicket::getStatus, "ASSIGNED", "PROCESSING");
        return faultTicketMapper.selectCount(wrapper).intValue();
    }

    private int countStationFaultPiles(Long stationId) {
        LambdaQueryWrapper<Pile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pile::getStationId, stationId);
        wrapper.eq(Pile::getStatus, "FAULT");
        return pileMapper.selectCount(wrapper).intValue();
    }

    private int countWaitingOwners(Long stationId) {
        LambdaQueryWrapper<Pile> pileWrapper = new LambdaQueryWrapper<>();
        pileWrapper.eq(Pile::getStationId, stationId);
        pileWrapper.eq(Pile::getStatus, "CHARGING");
        return pileMapper.selectCount(pileWrapper).intValue() * 2;
    }

    private BigDecimal calculateStationMonthlyRevenue(Long stationId) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        LambdaQueryWrapper<ChargingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChargingOrder::getStationId, stationId);
        wrapper.ge(ChargingOrder::getEndTime, oneMonthAgo);
        wrapper.in(ChargingOrder::getStatus, "COMPLETED", "INTERRUPTED", "FAULT_INTERRUPT");
        List<ChargingOrder> orders = orderMapper.selectList(wrapper);

        return orders.stream()
                .map(o -> o.getTotalFee() != null ? o.getTotalFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateRouteDescription(FaultTicket ticket, EngineerRecommendationDTO engineer) {
        Station station = stationMapper.selectById(ticket.getStationId());
        String stationName = station != null ? station.getName() : "未知站点";
        return String.format("工程师[%s]预计%d分钟后到达%s，总修复时间约%d分钟",
                engineer.getEngineerName(),
                engineer.getEstimatedArrivalMinutes(),
                stationName,
                engineer.getEstimatedArrivalMinutes() + estimateRepairMinutes(ticket.getFaultType()));
    }

    private RepairDispatchResultDTO buildDispatchResult(FaultTicket ticket,
                                                         List<EngineerRecommendationDTO> recommendations) {
        RepairDispatchResultDTO result = new RepairDispatchResultDTO();
        result.setFaultTicketId(ticket.getId());
        result.setStationId(ticket.getStationId());
        result.setPileId(ticket.getPileId());
        result.setPriorityScore(ticket.getPriorityScore());
        result.setEstimatedRestoreTime(ticket.getEstimatedRestoreTime());
        result.setFaultPileCount(ticket.getFaultPileCount());
        result.setWaitingCarOwners(ticket.getWaitingCarOwners());
        result.setStationRevenue(ticket.getStationRevenue());
        result.setSiteOwnerRequest(ticket.getSiteOwnerRequest());
        result.setPartsDistance(ticket.getPartsDistance());
        result.setEngineerRecommendations(recommendations);
        result.setRecommendedRoute(ticket.getRecommendedRoute());

        Station station = stationMapper.selectById(ticket.getStationId());
        if (station != null) {
            result.setStationName(station.getName());
        }
        Pile pile = pileMapper.selectById(ticket.getPileId());
        if (pile != null) {
            result.setPileCode(pile.getPileCode());
        }

        return result;
    }

    private FaultRecoveryStatusDTO buildRecoveryStatus(FaultTicket ticket) {
        FaultRecoveryStatusDTO dto = new FaultRecoveryStatusDTO();
        dto.setFaultTicketId(ticket.getId());
        dto.setStationId(ticket.getStationId());
        dto.setPileId(ticket.getPileId());
        dto.setStatus(ticket.getStatus());
        dto.setAssignedEngineerId(ticket.getAssignedEngineerId());
        dto.setAssignedAt(ticket.getAssignedAt());
        dto.setEstimatedRestoreTime(ticket.getEstimatedRestoreTime());
        dto.setPriorityScore(ticket.getPriorityScore());

        if (ticket.getCreatedAt() != null) {
            dto.setWaitingMinutes((int) Duration.between(ticket.getCreatedAt(), LocalDateTime.now()).toMinutes());
        }

        Station station = stationMapper.selectById(ticket.getStationId());
        if (station != null) {
            dto.setStationName(station.getName());
        }
        Pile pile = pileMapper.selectById(ticket.getPileId());
        if (pile != null) {
            dto.setPileCode(pile.getPileCode());
        }
        if (ticket.getAssignedEngineerId() != null) {
            User engineer = userMapper.selectById(ticket.getAssignedEngineerId());
            if (engineer != null) {
                dto.setEngineerName(engineer.getName());
            }
        }

        return dto;
    }
}
