package com.charging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompensationDecisionRequest {

    @NotNull(message = "补偿记录ID不能为空")
    private Long compensationId;

    @NotBlank(message = "决定不能为空")
    private String decision;

    private Long switchTargetPileId;
}
