package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("power_outage_notice")
public class PowerOutageNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stationId;

    private Long siteOwnerId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String reason;

    private String status;

    private Integer affectedOrderCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
