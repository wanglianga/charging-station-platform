package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OutageAffectedOrderDTO {

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal chargedKwh;

    private BigDecimal totalFee;

    private String status;
}
