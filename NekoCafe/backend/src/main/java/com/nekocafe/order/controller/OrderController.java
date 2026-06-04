package com.nekocafe.order.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.order.service.OrderService;
import com.nekocafe.order.service.OrderService.CreateOrderRequest;
import com.nekocafe.order.service.OrderService.OrderResponse;
import com.nekocafe.security.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "order", "status", "ready"));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<OrderResponse> create(@AuthenticationPrincipal AuthPrincipal principal, @RequestBody CreateOrderRequest request) {
        return ApiResult.ok(orderService.create(principal.userId(), request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<List<OrderResponse>> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(orderService.mine(principal.userId()));
    }
}
