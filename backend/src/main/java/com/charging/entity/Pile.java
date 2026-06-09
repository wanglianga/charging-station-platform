package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pile")
public class Pile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stationId;

    private String pileCode;

    private BigDecimal power;

    private String type;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
