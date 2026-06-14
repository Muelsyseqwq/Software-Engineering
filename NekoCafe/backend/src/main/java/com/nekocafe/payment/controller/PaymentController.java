package com.nekocafe.payment.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.payment.service.PaymentService;
import com.nekocafe.payment.service.PaymentService.PaymentResponse;
import com.nekocafe.payment.service.PaymentService.SandboxPaymentRequest;
import com.nekocafe.security.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "payment", "status", "ready"));
    }

    @PostMapping("/sandbox")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<PaymentResponse> sandboxPay(@AuthenticationPrincipal AuthPrincipal principal, @RequestBody SandboxPaymentRequest request) {
        return ApiResult.ok(paymentService.sandboxPay(principal.userId(), request));
    }
}
