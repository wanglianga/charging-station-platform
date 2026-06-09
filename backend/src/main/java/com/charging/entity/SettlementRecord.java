package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement_record")
public class SettlementRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long stationId;

    private BigDecimal totalElectricityFee;

    private BigDecimal totalServiceFee;

    private BigDecimal operatorShare;

    private BigDecimal siteOwnerShare;

    private BigDecimal propertyShare;

    private String status;

    private String period;

    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime updatedAt;
}
