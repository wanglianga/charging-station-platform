package com.charging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FaultTicketRequest {

    @NotNull(message = "站点ID不能为空")
    private Long stationId;

    @NotNull(message = "充电桩ID不能为空")
    private Long pileId;

    @NotBlank(message = "故障类型不能为空")
    private String faultType;

    private String severity;

    private String description;
}
