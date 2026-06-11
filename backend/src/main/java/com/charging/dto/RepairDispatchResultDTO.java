package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepairDispatchResultDTO {

    private Long faultTicketId;

    private Long stationId;

    private String stationName;

    private Long pileId;

    private String pileCode;

    private BigDecimal priorityScore;

    private LocalDateTime estimatedRestoreTime;

    private Integer faultPileCount;

    private Integer waitingCarOwners;

    private BigDecimal stationRevenue;

    private String siteOwnerRequest;

    private BigDecimal partsDistance;

    private List<EngineerRecommendationDTO> engineerRecommendations;

    private String recommendedRoute;
}
