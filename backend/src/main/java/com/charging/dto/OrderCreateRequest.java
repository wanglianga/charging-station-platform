package com.charging.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateRequest {

    @NotNull(message = "充电桩ID不能为空")
    private Long pileId;
}
