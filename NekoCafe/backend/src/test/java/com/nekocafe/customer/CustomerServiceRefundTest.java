package com.nekocafe.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.activity.mapper.ActivityStoreMapper;
import com.nekocafe.activity.mapper.PromotionActivityMapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.customer.entity.RefundRequest;
import com.nekocafe.customer.mapper.PointsTransactionMapper;
import com.nekocafe.customer.mapper.RefundRequestMapper;
import com.nekocafe.customer.mapper.ReviewMapper;
import com.nekocafe.customer.mapper.RewardCatalogMapper;
import com.nekocafe.customer.mapper.RewardRedemptionMapper;
import com.nekocafe.customer.mapper.UserPreferenceMapper;
import com.nekocafe.customer.service.CustomerService;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.order.service.OrderService;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.user.mapper.MemberAccountMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceRefundTest {

    // Dummy mocks for CustomerService constructor (not used in refund flow)
    private final ReviewMapper reviewMapper = mock(ReviewMapper.class);
    private final RefundRequestMapper refundRequestMapper = mock(RefundRequestMapper.class);
    private final PointsTransactionMapper pointsTransactionMapper = mock(PointsTransactionMapper.class);
    private final UserPreferenceMapper userPreferenceMapper = mock(UserPreferenceMapper.class);
    private final MemberAccountMapper memberAccountMapper = mock(MemberAccountMapper.class);
    private final RewardCatalogMapper rewardCatalogMapper = mock(RewardCatalogMapper.class);
    private final RewardRedemptionMapper rewardRedemptionMapper = mock(RewardRedemptionMapper.class);
    private final PromotionActivityMapper promotionActivityMapper = mock(PromotionActivityMapper.class);
    private final ActivityStoreMapper activityStoreMapper = mock(ActivityStoreMapper.class);
    private final StoreMapper storeMapper = mock(StoreMapper.class);
    private final FoodOrderMapper orderMapper = mock(FoodOrderMapper.class);
    private final OrderService orderService = mock(OrderService.class);

    private final CustomerService customerService = new CustomerService(
        reviewMapper,
        refundRequestMapper,
        pointsTransactionMapper,
        userPreferenceMapper,
        memberAccountMapper,
        rewardCatalogMapper,
        rewardRedemptionMapper,
        promotionActivityMapper,
        activityStoreMapper,
        storeMapper,
        orderMapper,
        orderService);

    @Test
    @DisplayName("applyRefund: success — status PAID, refundStatus NONE, no prior refund request")
    void applyRefundSuccess() {
        FoodOrder order = order(1L, 1L, "PAID", "NONE", new BigDecimal("50.00"));
        when(orderService.getOwnedOrder(1L, 1L)).thenReturn(order);
        when(refundRequestMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(refundRequestMapper.insert(any(RefundRequest.class))).thenReturn(1);
        when(orderMapper.updateById(any(FoodOrder.class))).thenReturn(1);

        var payload = new CustomerService.RefundRequestPayload("不好吃");
        CustomerService.RefundResponse resp = customerService.applyRefund(1L, 1L, payload);

        assertThat(resp.status()).isEqualTo("APPLIED");

        // Verify order refundStatus updated to APPLIED in the DB
        ArgumentCaptor<FoodOrder> orderCaptor = ArgumentCaptor.forClass(FoodOrder.class);
        verify(orderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getRefundStatus()).isEqualTo("APPLIED");
    }

    @Test
    @DisplayName("applyRefund: duplicate fails — existing refund request for this order")
    void applyRefundDuplicateFails() {
        FoodOrder order = order(1L, 1L, "PAID", "NONE", new BigDecimal("50.00"));
        when(orderService.getOwnedOrder(1L, 1L)).thenReturn(order);
        when(refundRequestMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        var payload = new CustomerService.RefundRequestPayload("again");
        assertThatThrownBy(() -> customerService.applyRefund(1L, 1L, payload))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("已有退款处理记录");
    }

    @Test
    @DisplayName("applyRefund: wrong user fails — order belongs to userId=2, requester is userId=1")
    void applyRefundWrongUserFails() {
        // orderService.getOwnedOrder checks userId match; simulates order owned by user 2
        when(orderService.getOwnedOrder(1L, 1L))
            .thenThrow(new BizException(3006, "订单不存在"));

        var payload = new CustomerService.RefundRequestPayload("test");
        assertThatThrownBy(() -> customerService.applyRefund(1L, 1L, payload))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("订单不存在");
    }

    @Test
    @DisplayName("applyRefund: CREATED order fails — only PAID or PREPARING orders can refund")
    void applyRefundCreatedOrderFails() {
        FoodOrder order = order(1L, 1L, "CREATED", "NONE", new BigDecimal("50.00"));
        when(orderService.getOwnedOrder(1L, 1L)).thenReturn(order);

        var payload = new CustomerService.RefundRequestPayload("too early");
        assertThatThrownBy(() -> customerService.applyRefund(1L, 1L, payload))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("只有已支付或制作中的订单");
    }

    @Test
    @DisplayName("applyRefund: PREPARING order succeeds — PREPARING is a valid refundable status")
    void applyRefundPreparingOrderSucceeds() {
        FoodOrder order = order(1L, 1L, "PREPARING", "NONE", new BigDecimal("30.00"));
        when(orderService.getOwnedOrder(1L, 1L)).thenReturn(order);
        when(refundRequestMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(refundRequestMapper.insert(any(RefundRequest.class))).thenReturn(1);
        when(orderMapper.updateById(any(FoodOrder.class))).thenReturn(1);

        var payload = new CustomerService.RefundRequestPayload("等太久了");
        CustomerService.RefundResponse resp = customerService.applyRefund(1L, 1L, payload);

        assertThat(resp.status()).isEqualTo("APPLIED");
        verify(orderMapper).updateById(any(FoodOrder.class));
    }

    // ---- helper ----

    private static FoodOrder order(Long id, Long userId, String status, String refundStatus, BigDecimal payableAmount) {
        FoodOrder o = new FoodOrder();
        o.setId(id);
        o.setOrderNo("O20260613001");
        o.setUserId(userId);
        o.setStoreId(1L);
        o.setTotalAmount(new BigDecimal("50.00"));
        o.setPayableAmount(payableAmount);
        o.setStatus(status);
        o.setRefundStatus(refundStatus);
        o.setDeleted(0);
        return o;
    }
}
