package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SwitchablePileDTO {

    private Long pileId;

    private String pileCode;

    private BigDecimal power;

    private String type;

    private String status;
}
