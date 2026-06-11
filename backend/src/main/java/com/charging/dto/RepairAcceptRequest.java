package com.charging.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairAcceptRequest {

    @NotNull(message = "故障工单ID不能为空")
    private Long faultTicketId;

    private Long engineerId;

    private Integer estimatedRepairMinutes;
}
