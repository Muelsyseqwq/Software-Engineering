package com.nekocafe.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.customer.service.CustomerService;
import com.nekocafe.customer.service.RewardRedemptionService;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.service.OrderService;
import com.nekocafe.payment.entity.PaymentRecord;
import com.nekocafe.payment.mapper.PaymentRecordMapper;
import com.nekocafe.payment.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRecordMapper paymentRecordMapper = mock(PaymentRecordMapper.class);
    private final OrderService orderService = mock(OrderService.class);
    private final CustomerService customerService = mock(CustomerService.class);
    private final RewardRedemptionService rewardRedemptionService = mock(RewardRedemptionService.class);
    private final PaymentService paymentService = new PaymentService(
        paymentRecordMapper, orderService, customerService, rewardRedemptionService);

    @Test
    void sandboxPaySuccess() {
        FoodOrder order = foodOrder(1L, 1L, "CREATED", new BigDecimal("50.00"));

        when(orderService.getOwnedOrder(1L, 1L)).thenReturn(order);
        // validateReservationCanBePaid is void on mock — no-op by default
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(paymentRecordMapper.insert(any(PaymentRecord.class))).thenReturn(1);

        PaymentService.PaymentResponse response = paymentService.sandboxPay(
            1L, new PaymentService.SandboxPaymentRequest(1L, "idem-1"));

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.channel()).isEqualTo("SANDBOX");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.paymentNo()).isNotNull();
        assertThat(response.paidAt()).isNotNull();

        verify(orderService, times(1)).markPaid(order);
    }

    @Test
    void sandboxPayIdempotent() {
        FoodOrder order = foodOrder(1L, 1L, "CREATED", new BigDecimal("50.00"));
        PaymentRecord existing = paymentRecord(100L, 1L, "idem-2", new BigDecimal("50.00"));

        when(orderService.getOwnedOrder(1L, 1L)).thenReturn(order);
        // First selectOne returns null (no duplicate), then returns existing on second call
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(null)
            .thenReturn(existing);
        when(paymentRecordMapper.insert(any(PaymentRecord.class))).thenReturn(1);

        // First payment — creates new record, markPaid called
        paymentService.sandboxPay(1L, new PaymentService.SandboxPaymentRequest(1L, "idem-2"));

        // Second payment with same idempotencyKey — returns existing record, no double insert
        PaymentService.PaymentResponse response = paymentService.sandboxPay(
            1L, new PaymentService.SandboxPaymentRequest(1L, "idem-2"));

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.paymentNo()).isEqualTo("P20260613000001");

        // markPaid must be called exactly once (only on first invocation)
        verify(orderService, times(1)).markPaid(order);
        // insert must be called exactly once
        verify(paymentRecordMapper, times(1)).insert(any(PaymentRecord.class));
    }

    @Test
    void sandboxPayAwardsPoints() {
        FoodOrder order = foodOrder(1L, 1L, "CREATED", new BigDecimal("30.00"));

        when(orderService.getOwnedOrder(1L, 1L)).thenReturn(order);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(paymentRecordMapper.insert(any(PaymentRecord.class))).thenReturn(1);

        paymentService.sandboxPay(1L, new PaymentService.SandboxPaymentRequest(1L, "idem-3"));

        verify(customerService, times(1)).awardPointsForPaidOrder(order);
    }

    // --- helpers ---

    private static FoodOrder foodOrder(Long id, Long userId, String status, BigDecimal payableAmount) {
        FoodOrder order = new FoodOrder();
        order.setId(id);
        order.setOrderNo("O20260613001");
        order.setUserId(userId);
        order.setStoreId(1L);
        order.setTotalAmount(payableAmount);
        order.setPayableAmount(payableAmount);
        order.setStatus(status);
        order.setRefundStatus("NONE");
        order.setDeleted(0);
        return order;
    }

    private static PaymentRecord paymentRecord(Long id, Long orderId, String idempotencyKey, BigDecimal amount) {
        PaymentRecord record = new PaymentRecord();
        record.setId(id);
        record.setPaymentNo("P20260613000001");
        record.setOrderId(orderId);
        record.setIdempotencyKey(idempotencyKey);
        record.setAmount(amount);
        record.setChannel("SANDBOX");
        record.setStatus("SUCCESS");
        record.setPaidAt(LocalDateTime.of(2026, 6, 13, 12, 0, 0));
        return record;
    }
}
