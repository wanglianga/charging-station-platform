package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("meter_reconciliation")
public class MeterReconciliation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stationId;

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

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
