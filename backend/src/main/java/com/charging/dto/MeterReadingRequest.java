package com.charging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MeterReadingRequest {

    @NotNull(message = "站点ID不能为空")
    private Long stationId;

    @NotBlank(message = "读数类型不能为空")
    private String readingType;

    @NotNull(message = "电量不能为空")
    private BigDecimal kwh;

    @NotNull(message = "读数日期不能为空")
    private LocalDate readingDate;
}
