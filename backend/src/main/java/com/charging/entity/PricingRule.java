package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pricing_rule")
public class PricingRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stationId;

    private BigDecimal peakPrice;

    private BigDecimal flatPrice;

    private BigDecimal valleyPrice;

    private String peakHours;

    private String flatHours;

    private String valleyHours;

    private BigDecimal serviceFeeRate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
