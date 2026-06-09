package com.charging.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charging.common.Result;
import com.charging.dto.OrderCompleteRequest;
import com.charging.dto.OrderCreateRequest;
import com.charging.dto.OrderInterruptRequest;
import com.charging.dto.RefundRequestDTO;
import com.charging.entity.ChargingOrder;
import com.charging.entity.RefundRequest;
import com.charging.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Result<Page<ChargingOrder>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) {
        return Result.ok(orderService.listOrders(page, size, status, userId));
    }

    @GetMapping("/{id}")
    public Result<ChargingOrder> getOrder(@PathVariable Long id) {
        return Result.ok(orderService.getOrderById(id));
    }

    @PostMapping
    public Result<ChargingOrder> createOrder(Authentication authentication,
                                             @Valid @RequestBody OrderCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(orderService.createOrder(userId, request));
    }

    @PostMapping("/{id}/start")
    public Result<ChargingOrder> startCharging(@PathVariable Long id) {
        return Result.ok(orderService.startCharging(id));
    }

    @PostMapping("/{id}/complete")
    public Result<ChargingOrder> completeCharging(@PathVariable Long id,
                                                  @RequestBody OrderCompleteRequest request) {
        return Result.ok(orderService.completeCharging(id, request));
    }

    @PostMapping("/{id}/interrupt")
    public Result<ChargingOrder> interruptCharging(@PathVariable Long id,
                                                    @RequestBody OrderInterruptRequest request) {
        return Result.ok(orderService.interruptCharging(id, request));
    }

    @PostMapping("/refund")
    public Result<RefundRequest> requestRefund(Authentication authentication,
                                                @Valid @RequestBody RefundRequestDTO request) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(orderService.requestRefund(userId, request));
    }
}
