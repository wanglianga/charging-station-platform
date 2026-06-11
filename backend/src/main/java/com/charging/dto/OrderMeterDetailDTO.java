package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderMeterDetailDTO {

    private Long orderId;

    private String orderNo;

    private Long pileId;

    private String pileCode;

    private BigDecimal chargedKwh;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;
}
