package com.charging.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.FaultAssignRequest;
import com.charging.dto.FaultTicketRequest;
import com.charging.entity.FaultTicket;

public interface FaultService {

    Page<FaultTicket> listFaults(int page, int size, String status, String faultType);

    FaultTicket getFaultById(Long id);

    FaultTicket createFaultTicket(FaultTicketRequest request);

    FaultTicket assignFault(Long id, FaultAssignRequest request);

    FaultTicket resolveFault(Long id, String description);

    FaultTicket closeFault(Long id);
}
