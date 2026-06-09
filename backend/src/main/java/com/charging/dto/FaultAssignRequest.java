package com.charging.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FaultAssignRequest {

    @NotNull(message = "工程师ID不能为空")
    private Long engineerId;
}
