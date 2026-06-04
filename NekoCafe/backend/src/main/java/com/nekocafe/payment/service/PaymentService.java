package com.nekocafe.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.service.OrderService;
import com.nekocafe.payment.entity.PaymentRecord;
import com.nekocafe.payment.mapper.PaymentRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PaymentService {

    private static final String SANDBOX = "SANDBOX";
    private static final String SUCCESS = "SUCCESS";
    private static final String PAID = "PAID";

    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderService orderService;

    public PaymentService(PaymentRecordMapper paymentRecordMapper, OrderService orderService) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.orderService = orderService;
    }

    @Transactional
    public PaymentResponse sandboxPay(Long userId, SandboxPaymentRequest request) {
        if (request == null || request.orderId() == null) {
            throw new BizException(3101, "请选择要支付的订单");
        }
        FoodOrder order = orderService.getOwnedOrder(userId, request.orderId());
        if (PAID.equals(order.getStatus())) {
            PaymentRecord existing = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, order.getId())
                .eq(PaymentRecord::getStatus, SUCCESS)
                .orderByDesc(PaymentRecord::getId)
                .last("LIMIT 1"));
            if (existing != null) {
                return toResponse(existing);
            }
        }

        String key = normalizeOptional(request.idempotencyKey());
        if (key == null) {
            key = "sandbox-" + order.getId();
        }
        PaymentRecord existingByKey = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
            .eq(PaymentRecord::getIdempotencyKey, key)
            .last("LIMIT 1"));
        if (existingByKey != null) {
            return toResponse(existingByKey);
        }

        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo(generatePaymentNo(userId));
        record.setOrderId(order.getId());
        record.setIdempotencyKey(key);
        record.setAmount(order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
        record.setChannel(SANDBOX);
        record.setStatus(SUCCESS);
        record.setPaidAt(LocalDateTime.now());
        paymentRecordMapper.insert(record);
        orderService.markPaid(order);
        return toResponse(record);
    }

    private PaymentResponse toResponse(PaymentRecord record) {
        return new PaymentResponse(
            record.getId(),
            record.getPaymentNo(),
            record.getOrderId(),
            record.getAmount(),
            record.getChannel(),
            record.getStatus(),
            record.getPaidAt()
        );
    }

    private String generatePaymentNo(Long userId) {
        return "P" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId;
    }

    private String normalizeOptional(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record SandboxPaymentRequest(Long orderId, String idempotencyKey) {
    }

    public record PaymentResponse(Long id, String paymentNo, Long orderId, BigDecimal amount, String channel, String status, LocalDateTime paidAt) {
    }
}
