package com.charging.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCompleteRequest {

    private BigDecimal endKwh;
}
