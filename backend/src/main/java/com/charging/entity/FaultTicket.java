package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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

    private LocalDateTime updatedAt;
}
