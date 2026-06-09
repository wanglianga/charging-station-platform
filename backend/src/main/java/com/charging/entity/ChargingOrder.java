package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("charging_order")
public class ChargingOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long pileId;

    private Long stationId;

    private String status;

    private BigDecimal startKwh;

    private BigDecimal endKwh;

    private BigDecimal chargedKwh;

    private BigDecimal electricityFee;

    private BigDecimal serviceFee;

    private BigDecimal totalFee;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String interruptReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
