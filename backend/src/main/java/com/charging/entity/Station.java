package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("station")
public class Station {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String status;

    private Long siteOwnerId;

    private BigDecimal commissionRate;

    private BigDecimal propertyShareRate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
