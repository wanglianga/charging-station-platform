package com.charging.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MeterReconciliationRequest {

    @NotNull(message = "站点ID不能为空")
    private Long stationId;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotNull(message = "总表读数不能为空")
    private BigDecimal totalMeterKwh;

    private BigDecimal subMeterTotalKwh;
}
