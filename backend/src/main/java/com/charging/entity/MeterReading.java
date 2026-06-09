package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("meter_reading")
public class MeterReading {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stationId;

    private String readingType;

    private BigDecimal kwh;

    private LocalDate readingDate;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    private LocalDateTime createdAt;
}
