package com.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_request")
public class RefundRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private String reason;

    private String status;

    private Long processedBy;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private LocalDateTime updatedAt;
}
