package com.charging.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.dto.OrderCreateRequest;
import com.charging.dto.OrderCompleteRequest;
import com.charging.dto.OrderInterruptRequest;
import com.charging.dto.RefundRequestDTO;
import com.charging.entity.ChargingOrder;
import com.charging.entity.RefundRequest;

public interface OrderService {

    Page<ChargingOrder> listOrders(int page, int size, String status, Long userId);

    ChargingOrder getOrderById(Long id);

    ChargingOrder createOrder(Long userId, OrderCreateRequest request);

    ChargingOrder startCharging(Long orderId);

    ChargingOrder completeCharging(Long orderId, OrderCompleteRequest request);

    ChargingOrder interruptCharging(Long orderId, OrderInterruptRequest request);

    RefundRequest requestRefund(Long userId, RefundRequestDTO request);
}
