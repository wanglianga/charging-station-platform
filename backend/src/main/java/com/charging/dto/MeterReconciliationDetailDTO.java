package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MeterReconciliationDetailDTO {

    private Long id;

    private Long stationId;

    private String stationName;

    private String period;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal totalMeterKwh;

    private BigDecimal subMeterTotalKwh;

    private BigDecimal orderTotalKwh;

    private BigDecimal differenceKwh;

    private BigDecimal differenceRate;

    private String status;

    private String reviewNote;

    private String lossReason;

    private Long reviewedBy;

    private String reviewerName;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private List<PileMeterDetailDTO> pileMeterDetails;

    private List<OrderMeterDetailDTO> orderMeterDetails;
}
