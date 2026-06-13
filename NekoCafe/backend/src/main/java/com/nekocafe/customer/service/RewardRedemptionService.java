package com.nekocafe.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.customer.entity.RewardCatalog;
import com.nekocafe.customer.entity.RewardRedemption;
import com.nekocafe.customer.mapper.RewardCatalogMapper;
import com.nekocafe.customer.mapper.RewardRedemptionMapper;
import com.nekocafe.order.entity.FoodOrder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class RewardRedemptionService {

    private static final String ACTIVE = "ACTIVE";
    private static final String COUPON = "COUPON";
    private static final String REDEEMED = "REDEEMED";
    private static final String LOCKED = "LOCKED";
    private static final String USED = "USED";

    private final RewardRedemptionMapper rewardRedemptionMapper;
    private final RewardCatalogMapper rewardCatalogMapper;

    public RewardRedemptionService(RewardRedemptionMapper rewardRedemptionMapper, RewardCatalogMapper rewardCatalogMapper) {
        this.rewardRedemptionMapper = rewardRedemptionMapper;
        this.rewardCatalogMapper = rewardCatalogMapper;
    }

    public CouponUsage prepareForOrder(Long userId, Long redemptionId, BigDecimal orderTotal) {
        if (redemptionId == null) {
            return null;
        }
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        RewardRedemption redemption = loadOwnedRedemption(userId, redemptionId);
        if (!REDEEMED.equals(redemption.getStatus()) || redemption.getUsedAt() != null || redemption.getOrderId() != null) {
            throw new BizException(3345, "该兑换券已使用或已绑定订单");
        }
        RewardCatalog reward = rewardCatalogMapper.selectById(redemption.getRewardId());
        validateCouponReward(reward);
        BigDecimal configuredDiscount = reward.getDiscountAmount() == null ? BigDecimal.TEN : reward.getDiscountAmount();
        BigDecimal total = orderTotal == null ? BigDecimal.ZERO : orderTotal;
        BigDecimal discount = configuredDiscount.min(total).max(BigDecimal.ZERO);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(3346, "当前订单金额不能使用该兑换券");
        }
        return new CouponUsage(redemption.getId(), redemption.getRewardName(), discount);
    }

    public void bindToOrder(Long userId, CouponUsage usage, Long orderId) {
        if (usage == null) {
            return;
        }
        int updated = rewardRedemptionMapper.update(null, new LambdaUpdateWrapper<RewardRedemption>()
            .eq(RewardRedemption::getId, usage.redemptionId())
            .eq(RewardRedemption::getUserId, userId)
            .eq(RewardRedemption::getStatus, REDEEMED)
            .isNull(RewardRedemption::getUsedAt)
            .isNull(RewardRedemption::getOrderId)
            .set(RewardRedemption::getStatus, LOCKED)
            .set(RewardRedemption::getOrderId, orderId));
        if (updated == 0) {
            throw new BizException(3345, "该兑换券已使用或已绑定订单");
        }
    }

    public void markUsedForPaidOrder(FoodOrder order) {
        if (order == null || order.getRewardRedemptionId() == null) {
            return;
        }
        RewardRedemption redemption = rewardRedemptionMapper.selectById(order.getRewardRedemptionId());
        if (redemption == null || Objects.equals(redemption.getDeleted(), 1)
            || !Objects.equals(redemption.getUserId(), order.getUserId())
            || !Objects.equals(redemption.getOrderId(), order.getId())) {
            throw new BizException(3347, "订单兑换券校验失败");
        }
        if (USED.equals(redemption.getStatus()) && redemption.getUsedAt() != null) {
            return;
        }
        if (!List.of(REDEEMED, LOCKED).contains(redemption.getStatus()) || redemption.getUsedAt() != null) {
            throw new BizException(3345, "该兑换券已使用或已绑定订单");
        }
        int updated = rewardRedemptionMapper.update(null, new LambdaUpdateWrapper<RewardRedemption>()
            .eq(RewardRedemption::getId, redemption.getId())
            .eq(RewardRedemption::getUserId, order.getUserId())
            .eq(RewardRedemption::getOrderId, order.getId())
            .in(RewardRedemption::getStatus, List.of(REDEEMED, LOCKED))
            .isNull(RewardRedemption::getUsedAt)
            .set(RewardRedemption::getStatus, USED)
            .set(RewardRedemption::getUsedAt, LocalDateTime.now()));
        if (updated == 0) {
            RewardRedemption latest = rewardRedemptionMapper.selectById(redemption.getId());
            if (latest != null && USED.equals(latest.getStatus()) && Objects.equals(latest.getOrderId(), order.getId())) {
                return;
            }
            throw new BizException(3345, "该兑换券已使用或已绑定订单");
        }
    }

    private RewardRedemption loadOwnedRedemption(Long userId, Long redemptionId) {
        RewardRedemption redemption = rewardRedemptionMapper.selectOne(new LambdaQueryWrapper<RewardRedemption>()
            .eq(RewardRedemption::getId, redemptionId)
            .eq(RewardRedemption::getUserId, userId)
            .eq(RewardRedemption::getDeleted, 0)
            .last("LIMIT 1"));
        if (redemption == null) {
            throw new BizException(3345, "兑换券不存在或不属于当前账号");
        }
        return redemption;
    }

    private void validateCouponReward(RewardCatalog reward) {
        if (reward == null || Objects.equals(reward.getDeleted(), 1) || !COUPON.equals(reward.getRewardType())) {
            throw new BizException(3348, "该兑换记录不是可用于结算的优惠券");
        }
        if (!ACTIVE.equals(reward.getStatus())) {
            throw new BizException(3348, "该优惠券暂不可用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (reward.getValidFrom() != null && reward.getValidFrom().isAfter(now)) {
            throw new BizException(3348, "该优惠券还未生效");
        }
        if (reward.getValidTo() != null && reward.getValidTo().isBefore(now)) {
            throw new BizException(3348, "该优惠券已过期");
        }
    }

    public record CouponUsage(Long redemptionId, String rewardName, BigDecimal discountAmount) {
    }

    public void releaseLockedForOrder(FoodOrder order) {
        if (order == null || order.getRewardRedemptionId() == null) {
            return;
        }
        rewardRedemptionMapper.update(null, new LambdaUpdateWrapper<RewardRedemption>()
            .eq(RewardRedemption::getId, order.getRewardRedemptionId())
            .eq(RewardRedemption::getUserId, order.getUserId())
            .eq(RewardRedemption::getOrderId, order.getId())
            .eq(RewardRedemption::getStatus, LOCKED)
            .isNull(RewardRedemption::getUsedAt)
            .set(RewardRedemption::getStatus, REDEEMED)
            .set(RewardRedemption::getOrderId, null));
    }
}
