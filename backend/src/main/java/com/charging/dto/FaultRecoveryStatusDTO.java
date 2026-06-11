package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FaultRecoveryStatusDTO {

    private Long faultTicketId;

    private Long stationId;

    private String stationName;

    private Long pileId;

    private String pileCode;

    private String status;

    private Long assignedEngineerId;

    private String engineerName;

    private LocalDateTime assignedAt;

    private LocalDateTime estimatedRestoreTime;

    private BigDecimal priorityScore;

    private Integer waitingMinutes;
}
