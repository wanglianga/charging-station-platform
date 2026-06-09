package com.charging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PileRequest {

    @NotNull(message = "站点ID不能为空")
    private Long stationId;

    @NotBlank(message = "桩编号不能为空")
    private String pileCode;

    @NotNull(message = "功率不能为空")
    private BigDecimal power;

    @NotBlank(message = "类型不能为空")
    private String type;
}
