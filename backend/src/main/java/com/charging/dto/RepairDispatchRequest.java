package com.charging.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepairDispatchRequest {

    @NotNull(message = "故障工单ID不能为空")
    private Long faultTicketId;

    private Integer faultPileCount;

    private Integer waitingCarOwners;

    private BigDecimal stationRevenue;

    private String siteOwnerRequest;

    private BigDecimal partsDistance;
}
