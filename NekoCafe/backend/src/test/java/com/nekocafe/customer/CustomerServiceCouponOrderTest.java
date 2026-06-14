package com.nekocafe.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.activity.entity.PromotionActivity;
import com.nekocafe.activity.mapper.ActivityStoreMapper;
import com.nekocafe.activity.mapper.PromotionActivityMapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.customer.entity.PointsTransaction;
import com.nekocafe.customer.entity.RewardCatalog;
import com.nekocafe.customer.entity.RewardRedemption;
import com.nekocafe.customer.entity.UserPreference;
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
import com.nekocafe.user.entity.MemberAccount;
import com.nekocafe.user.mapper.MemberAccountMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceCouponOrderTest {

    private final ReviewMapper reviewMapper = mock(ReviewMapper.class);
    private final RefundRequestMapper refundRequestMapper = mock(RefundRequestMapper.class);
    private final PointsTransactionMapper pointsTransactionMapper = mock(PointsTransactionMapper.class);
    private final UserPreferenceMapper userPreferenceMapper = mock(UserPreferenceMapper.class);
    private final MemberAccountMapper memberAccountMapper = mock(MemberAccountMapper.class);
    private final RewardCatalogMapper rewardCatalogMapper = mock(RewardCatalogMapper.class);
    private final RewardRedemptionMapper rewardRedemptionMapper = mock(RewardRedemptionMapper.class);
    private final PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
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
        activityMapper,
        activityStoreMapper,
        storeMapper,
        orderMapper,
        orderService);

    // ── claimCoupon / claimActivityReward ──────────────────────────────────

    @Test
    @DisplayName("claimCoupon: success — valid published activity with COUPON reward, no prior claim")
    void claimCouponSuccess() {
        PromotionActivity activity = activity(1L, "PUBLISHED", 100L);
        RewardCatalog reward = rewardCatalog(100L, "COUPON", "满50减10");

        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activity);
        when(rewardCatalogMapper.selectById(100L)).thenReturn(reward);
        when(rewardRedemptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(memberAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(memberAccount(1L, 1L, "NORMAL", 100));
        when(rewardRedemptionMapper.insert(any(RewardRedemption.class))).thenReturn(1);

        CustomerService.RewardRedemptionResponse resp = customerService.claimActivityReward(1L, 1L);

        assertThat(resp.status()).isEqualTo("REDEEMED");
        assertThat(resp.rewardName()).isEqualTo("满50减10");
        assertThat(resp.sourceType()).isEqualTo("ACTIVITY");
        assertThat(resp.sourceId()).isEqualTo(1L);
        verify(rewardRedemptionMapper).insert(any(RewardRedemption.class));
    }

    @Test
    @DisplayName("claimCoupon: already claimed fails — duplicate redemption detected")
    void claimCouponAlreadyClaimedFails() {
        PromotionActivity activity = activity(1L, "PUBLISHED", 100L);
        RewardCatalog reward = rewardCatalog(100L, "COUPON", "满50减10");

        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activity);
        when(rewardCatalogMapper.selectById(100L)).thenReturn(reward);
        when(rewardRedemptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> customerService.claimActivityReward(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("已领取该活动优惠券");
    }

    @Test
    @DisplayName("claimCoupon: activity not found fails")
    void claimCouponActivityNotFoundFails() {
        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> customerService.claimActivityReward(1L, 999L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("活动不存在");
    }

    @Test
    @DisplayName("claimCoupon: activity not published fails")
    void claimCouponActivityNotPublishedFails() {
        PromotionActivity activity = activity(1L, "DRAFT", 100L);
        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activity);

        assertThatThrownBy(() -> customerService.claimActivityReward(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("活动暂未发布");
    }

    @Test
    @DisplayName("claimCoupon: no coupon reward fails — activity has no rewardId")
    void claimCouponNoRewardFails() {
        PromotionActivity activity = activity(1L, "PUBLISHED", null);
        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activity);

        // rewardId is null, so claim fails at validateActivityClaimable → rewardId == null check
        assertThatThrownBy(() -> customerService.claimActivityReward(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("暂无可领取优惠券");
    }

    @Test
    @DisplayName("claimCoupon: activity not started fails — startAt in future")
    void claimCouponActivityNotStartedFails() {
        PromotionActivity activity = activity(1L, "PUBLISHED", 100L,
            LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(7));
        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activity);

        assertThatThrownBy(() -> customerService.claimActivityReward(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("活动还未开始");
    }

    @Test
    @DisplayName("claimCoupon: activity ended fails — endAt in past")
    void claimCouponActivityEndedFails() {
        PromotionActivity activity = activity(1L, "PUBLISHED", 100L,
            LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(1));
        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activity);

        assertThatThrownBy(() -> customerService.claimActivityReward(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("活动已结束");
    }

    @Test
    @DisplayName("claimCoupon: reward not COUPON type fails")
    void claimCouponRewardNotCouponTypeFails() {
        PromotionActivity activity = activity(1L, "PUBLISHED", 100L);
        RewardCatalog reward = rewardCatalog(100L, "POINTS", "积分加倍", "ACTIVE");
        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activity);
        when(rewardCatalogMapper.selectById(100L)).thenReturn(reward);

        assertThatThrownBy(() -> customerService.claimActivityReward(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("暂无可领取优惠券");
    }

    @Test
    @DisplayName("claimCoupon: reward not found in catalog fails")
    void claimCouponRewardNotFoundFails() {
        PromotionActivity activity = activity(1L, "PUBLISHED", 100L);
        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activity);
        when(rewardCatalogMapper.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() -> customerService.claimActivityReward(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("奖励不存在或已下架");
    }

    @Test
    @DisplayName("claimCoupon: reward not ACTIVE fails")
    void claimCouponRewardNotActiveFails() {
        PromotionActivity activity = activity(1L, "PUBLISHED", 100L);
        RewardCatalog reward = rewardCatalog(100L, "COUPON", "满50减10", "INACTIVE");
        when(activityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activity);
        when(rewardCatalogMapper.selectById(100L)).thenReturn(reward);

        assertThatThrownBy(() -> customerService.claimActivityReward(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("奖励暂不可兑换");
    }

    @Test
    @DisplayName("claimCoupon: not logged in fails — userId is null")
    void claimCouponNotLoggedInFails() {
        assertThatThrownBy(() -> customerService.claimActivityReward(null, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("请先登录");
    }

    // ── getMyCoupons / myRedemptions ───────────────────────────────────────

    @Test
    @DisplayName("getMyCoupons: returns redemptions list from mapper")
    void getMyCouponsReturnsList() {
        RewardRedemption r1 = redemption(1L, 1L, 100L, "满50减10", "REDEEMED", "ACTIVITY", 1L);
        RewardRedemption r2 = redemption(2L, 1L, 200L, "会员专享8折", "REDEEMED", "ACTIVITY", 2L);

        when(rewardRedemptionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(r1, r2));

        List<CustomerService.RewardRedemptionResponse> resp = customerService.myRedemptions(1L);

        assertThat(resp).hasSize(2);
        assertThat(resp.get(0).rewardName()).isEqualTo("满50减10");
        assertThat(resp.get(1).rewardName()).isEqualTo("会员专享8折");
    }

    @Test
    @DisplayName("getMyCoupons: returns empty list when no redemptions")
    void getMyCouponsEmptyList() {
        when(rewardRedemptionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());

        List<CustomerService.RewardRedemptionResponse> resp = customerService.myRedemptions(1L);

        assertThat(resp).isEmpty();
    }

    // ── getMyMemberRewards / rewards ───────────────────────────────────────

    @Test
    @DisplayName("getMyMemberRewards: returns available reward catalog")
    void getMyMemberRewardsReturnsCatalog() {
        RewardCatalog rc1 = rewardCatalog(1L, "COUPON", "满100减20");
        RewardCatalog rc2 = rewardCatalog(2L, "COUPON", "会员8折券");

        when(rewardCatalogMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(rc1, rc2));

        List<CustomerService.RewardCatalogResponse> resp = customerService.rewards(1L);

        assertThat(resp).hasSize(2);
        assertThat(resp.get(0).name()).isEqualTo("满100减20");
        assertThat(resp.get(1).name()).isEqualTo("会员8折券");
    }

    @Test
    @DisplayName("getMyMemberRewards: returns empty list when no rewards available")
    void getMyMemberRewardsEmptyList() {
        when(rewardCatalogMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());

        List<CustomerService.RewardCatalogResponse> resp = customerService.rewards(1L);

        assertThat(resp).isEmpty();
    }

    // ── getMyOrders / home ─────────────────────────────────────────────────

    @Test
    @DisplayName("getMyOrders: home includes orders from orderService.mine")
    void getMyOrdersViaHome() {
        OrderService.OrderResponse orderResp = orderResponse(1L, "O20260613001", "PAID");
        when(orderService.mine(1L)).thenReturn(List.of(orderResp));

        // activities → empty (no mappings)
        when(activityStoreMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        // points
        when(memberAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(memberAccount(1L, 1L, "VIP", 600));
        when(pointsTransactionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        // preferences
        when(userPreferenceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        CustomerService.HomeResponse resp = customerService.home(1L);

        assertThat(resp.recentOrders()).hasSize(1);
        assertThat(resp.recentOrders().get(0).orderNo()).isEqualTo("O20260613001");
        assertThat(resp.recentOrders().get(0).status()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("getMyOrders: home returns empty orders and empty activities when nothing available")
    void getMyOrdersViaHomeEmptyOrders() {
        when(orderService.mine(1L)).thenReturn(List.of());
        when(activityStoreMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(memberAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(memberAccount(1L, 1L, "NORMAL", 0));
        when(pointsTransactionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(userPreferenceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        CustomerService.HomeResponse resp = customerService.home(1L);

        assertThat(resp.recentOrders()).isEmpty();
        assertThat(resp.activities()).isEmpty();
        assertThat(resp.points().points()).isEqualTo(0);
        assertThat(resp.points().levelCode()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("getMyOrders: home orderStats counts statuses correctly")
    void getMyOrdersViaHomeOrderStats() {
        OrderService.OrderResponse paidOrder = orderResponse(1L, "O001", "PAID");
        OrderService.OrderResponse preparingOrder = orderResponse(2L, "O002", "PREPARING");
        OrderService.OrderResponse completedOrder = orderResponse(3L, "O003", "COMPLETED");
        when(orderService.mine(1L)).thenReturn(List.of(paidOrder, preparingOrder, completedOrder));

        when(activityStoreMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(memberAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(memberAccount(1L, 1L, "NORMAL", 0));
        when(pointsTransactionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(userPreferenceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        CustomerService.HomeResponse resp = customerService.home(1L);

        assertThat(resp.orderStats().paid()).isEqualTo(1);
        assertThat(resp.orderStats().preparing()).isEqualTo(1);
        assertThat(resp.orderStats().completed()).isEqualTo(1);
        assertThat(resp.orderStats().pendingPayment()).isEqualTo(0);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static PromotionActivity activity(Long id, String status, Long rewardId) {
        return activity(id, status, rewardId,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7));
    }

    private static PromotionActivity activity(Long id, String status, Long rewardId,
                                               LocalDateTime startAt, LocalDateTime endAt) {
        PromotionActivity a = new PromotionActivity();
        a.setId(id);
        a.setTitle("测试活动");
        a.setType("PROMOTION");
        a.setDescription("活动描述");
        a.setStatus(status);
        a.setRewardId(rewardId);
        a.setDeleted(0);
        a.setStartAt(startAt);
        a.setEndAt(endAt);
        return a;
    }

    private static RewardCatalog rewardCatalog(Long id, String rewardType, String name) {
        return rewardCatalog(id, rewardType, name, "ACTIVE");
    }

    private static RewardCatalog rewardCatalog(Long id, String rewardType, String name, String status) {
        RewardCatalog r = new RewardCatalog();
        r.setId(id);
        r.setName(name);
        r.setRewardType(rewardType);
        r.setStatus(status);
        r.setPointsCost(100);
        r.setStock(999);
        r.setDiscountAmount(new BigDecimal("10"));
        r.setDeleted(0);
        return r;
    }

    private static RewardRedemption redemption(Long id, Long userId, Long rewardId,
                                               String rewardName, String status,
                                               String sourceType, Long sourceId) {
        RewardRedemption r = new RewardRedemption();
        r.setId(id);
        r.setRedemptionNo("D" + id);
        r.setUserId(userId);
        r.setMemberAccountId(1L);
        r.setRewardId(rewardId);
        r.setRewardName(rewardName);
        r.setPointsCost(0);
        r.setSourceType(sourceType);
        r.setSourceId(sourceId);
        r.setStatus(status);
        r.setRedeemedAt(LocalDateTime.now());
        r.setDeleted(0);
        return r;
    }

    private static MemberAccount memberAccount(Long id, Long userId, String levelCode, int points) {
        MemberAccount m = new MemberAccount();
        m.setId(id);
        m.setUserId(userId);
        m.setLevelCode(levelCode);
        m.setPoints(points);
        m.setTotalSpent(BigDecimal.ZERO);
        return m;
    }

    private static OrderService.OrderResponse orderResponse(Long id, String orderNo, String status) {
        return new OrderService.OrderResponse(
            id,                    // id
            orderNo,               // orderNo
            1L,                    // storeId
            "测试门店",             // storeName
            null,                  // tableId
            null,                  // tableNo
            null,                  // reservationId
            new BigDecimal("100"), // totalAmount
            null,                  // rewardRedemptionId
            null,                  // couponName
            BigDecimal.ZERO,       // couponDiscountAmount
            new BigDecimal("100"), // payableAmount
            status,                // status
            "NONE",                // refundStatus
            null,                  // remark
            null,                  // paidAt
            null,                  // completedAt
            null,                  // cancelledAt
            LocalDateTime.now(),   // createdAt
            "CREATED".equals(status),                     // canPay
            ("PAID".equals(status) || "PREPARING".equals(status)), // canRefund
            "COMPLETED".equals(status),                   // canReview
            false,                 // reviewed
            "CREATED".equals(status),                     // canCancel
            List.of()              // items
        );
    }
}
