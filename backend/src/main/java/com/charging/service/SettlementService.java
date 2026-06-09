package com.charging.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.entity.SettlementRecord;

public interface SettlementService {

    Page<SettlementRecord> listSettlements(int page, int size, String status, String period);

    SettlementRecord getSettlementById(Long id);

    SettlementRecord calculateSettlement(Long orderId);

    SettlementRecord confirmSettlement(Long id, String status);
}
