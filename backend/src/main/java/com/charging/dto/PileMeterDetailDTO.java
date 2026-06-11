package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PileMeterDetailDTO {

    private Long pileId;

    private String pileCode;

    private BigDecimal startKwh;

    private BigDecimal endKwh;

    private BigDecimal consumedKwh;
}
