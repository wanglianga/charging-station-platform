package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InterruptionDetailDTO {

    private Long compensationId;

    private Long orderId;

    private String orderNo;

    private BigDecimal chargedKwh;

    private BigDecimal electricityFee;

    private BigDecimal serviceFee;

    private BigDecimal totalFee;

    private String stopReason;

    private Integer waitingMinutes;

    private List<SwitchablePileDTO> switchablePiles;

    private String decision;
}
