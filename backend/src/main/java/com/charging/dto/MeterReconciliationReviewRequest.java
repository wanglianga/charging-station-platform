package com.charging.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MeterReconciliationReviewRequest {

    @NotBlank(message = "审核状态不能为空")
    private String status;

    private String reviewNote;

    private String lossReason;
}
