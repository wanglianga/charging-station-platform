package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("interruption_compensation")
public class InterruptionCompensation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    private Long stationId;

    private Long pileId;

    private BigDecimal chargedKwh;

    private String stopReason;

    private Integer waitingMinutes;

    private String switchablePiles;

    private String decision;

    private Long switchTargetPileId;

    private Long switchOrderId;

    private LocalDateTime createdAt;

    private LocalDateTime decidedAt;

    private LocalDateTime updatedAt;
}
