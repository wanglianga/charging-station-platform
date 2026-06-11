package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EngineerRecommendationDTO {

    private Long engineerId;

    private String engineerName;

    private String engineerPhone;

    private BigDecimal distance;

    private Integer estimatedArrivalMinutes;

    private LocalDateTime estimatedArrivalTime;

    private LocalDateTime estimatedRestoreTime;

    private Integer currentWorkload;

    private BigDecimal matchScore;
}
