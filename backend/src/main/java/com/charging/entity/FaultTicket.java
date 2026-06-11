package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fault_ticket")
public class FaultTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stationId;

    private Long pileId;

    private String faultType;

    private String severity;

    private String status;

    private String description;

    private Long assignedEngineerId;

    private LocalDateTime createdAt;

    private LocalDateTime assignedAt;

    private LocalDateTime resolvedAt;

    private BigDecimal priorityScore;

    private LocalDateTime estimatedRestoreTime;

    private Integer faultPileCount;

    private Integer waitingCarOwners;

    private BigDecimal stationRevenue;

    private String siteOwnerRequest;

    private BigDecimal partsDistance;

    private Long recommendedEngineerId;

    private String recommendedRoute;

    private LocalDateTime updatedAt;
}
