package com.charging.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.*;
import com.charging.entity.FaultTicket;

import java.util.List;

public interface FaultService {

    Page<FaultTicket> listFaults(int page, int size, String status, String faultType);

    FaultTicket getFaultById(Long id);

    FaultTicket createFaultTicket(FaultTicketRequest request);

    FaultTicket assignFault(Long id, FaultAssignRequest request);

    FaultTicket resolveFault(Long id, String description);

    FaultTicket closeFault(Long id);

    RepairDispatchResultDTO calculateDispatchPriority(RepairDispatchRequest request);

    List<RepairDispatchResultDTO> batchCalculateDispatchPriority(List<Long> faultTicketIds);

    FaultTicket acceptRepair(RepairAcceptRequest request);

    FaultRecoveryStatusDTO getRecoveryStatus(Long faultTicketId);

    List<FaultRecoveryStatusDTO> getStationRecoveryStatus(Long stationId);
}
