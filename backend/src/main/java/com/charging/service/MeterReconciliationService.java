package com.charging.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.MeterReconciliationDetailDTO;
import com.charging.dto.MeterReconciliationRequest;
import com.charging.dto.MeterReconciliationReviewRequest;
import com.charging.entity.MeterReconciliation;

public interface MeterReconciliationService {

    Page<MeterReconciliation> listReconciliations(int page, int size, String status, Long stationId, String period);

    MeterReconciliationDetailDTO getReconciliationDetail(Long id);

    MeterReconciliation createReconciliation(MeterReconciliationRequest request);

    MeterReconciliation reviewReconciliation(Long id, Long operatorId, MeterReconciliationReviewRequest request);
}
