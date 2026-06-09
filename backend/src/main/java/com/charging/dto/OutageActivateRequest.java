package com.charging.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OutageActivateRequest {

    @NotNull(message = "停电通知ID不能为空")
    private Long outageNoticeId;
}
