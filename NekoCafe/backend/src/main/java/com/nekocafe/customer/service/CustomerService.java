package com.nekocafe.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.activity.entity.ActivityStore;
import com.nekocafe.activity.entity.PromotionActivity;
import com.nekocafe.activity.mapper.ActivityStoreMapper;
import com.nekocafe.activity.mapper.PromotionActivityMapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.customer.entity.PointsTransaction;
import com.nekocafe.customer.entity.RefundRequest;
import com.nekocafe.customer.entity.Review;
import com.nekocafe.customer.entity.RewardCatalog;
import com.nekocafe.customer.entity.RewardRedemption;
import com.nekocafe.customer.entity.UserPreference;
import com.nekocafe.customer.mapper.PointsTransactionMapper;
import com.nekocafe.customer.mapper.RefundRequestMapper;
import com.nekocafe.customer.mapper.ReviewMapper;
import com.nekocafe.customer.mapper.RewardCatalogMapper;
import com.nekocafe.customer.mapper.RewardRedemptionMapper;
import com.nekocafe.customer.mapper.UserPreferenceMapper;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.order.service.OrderService;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.user.entity.MemberAccount;
import com.nekocafe.user.mapper.MemberAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private static final String COMPLETED = "COMPLETED";
    private static final String PAID = "PAID";
    private static final String PREPARING = "PREPARING";
    private static final String VISIBLE = "VISIBLE";
    private static final String APPLIED = "APPLIED";
    private static final String NONE = "NONE";
    private static final String EARN = "EARN";
    private static final String REDEEM = "REDEEM";
    private static final String ACTIVE = "ACTIVE";
    private static final String REDEEMED = "REDEEMED";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String ACCEPTED = "ACCEPTED";

    private final ReviewMapper reviewMapper;
    private final RefundRequestMapper refundRequestMapper;
    private final PointsTransactionMapper pointsTransactionMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final MemberAccountMapper memberAccountMapper;
    private final RewardCatalogMapper rewardCatalogMapper;
    private final RewardRedemptionMapper rewardRedemptionMapper;
    private final PromotionActivityMapper activityMapper;
    private final ActivityStoreMapper activityStoreMapper;
    private final StoreMapper storeMapper;
    private final FoodOrderMapper orderMapper;
    private final OrderService orderService;

    public CustomerService(
        ReviewMapper reviewMapper,
        RefundRequestMapper refundRequestMapper,
        PointsTransactionMapper pointsTransactionMapper,
        UserPreferenceMapper userPreferenceMapper,
        MemberAccountMapper memberAccountMapper,
        RewardCatalogMapper rewardCatalogMapper,
        RewardRedemptionMapper rewardRedemptionMapper,
        PromotionActivityMapper activityMapper,
        ActivityStoreMapper activityStoreMapper,
        StoreMapper storeMapper,
        FoodOrderMapper orderMapper,
        OrderService orderService
    ) {
        this.reviewMapper = reviewMapper;
        this.refundRequestMapper = refundRequestMapper;
        this.pointsTransactionMapper = pointsTransactionMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.memberAccountMapper = memberAccountMapper;
        this.rewardCatalogMapper = rewardCatalogMapper;
        this.rewardRedemptionMapper = rewardRedemptionMapper;
        this.activityMapper = activityMapper;
        this.activityStoreMapper = activityStoreMapper;
        this.storeMapper = storeMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    public HomeResponse home(Long userId) {
        ensureLogin(userId);
        List<OrderService.OrderResponse> orders = orderService.mine(userId);
        List<CustomerActivityResponse> activities = activities(null, null).stream().limit(4).toList();
        return new HomeResponse(
            points(userId),
            buildOrderStats(orders),
            orders.stream().limit(5).toList(),
            activities,
            preferences(userId)
        );
    }

    public List<CustomerActivityResponse> activities(String type, Long storeId) {
        LambdaQueryWrapper<ActivityStore> mappingWrapper = new LambdaQueryWrapper<ActivityStore>()
            .eq(ActivityStore::getAcceptStatus, ACCEPTED)
            .orderByDesc(ActivityStore::getHandledAt)
            .orderByDesc(ActivityStore::getId);
        if (storeId != null) {
            mappingWrapper.eq(ActivityStore::getStoreId, storeId);
        }
        List<ActivityStore> mappings = activityStoreMapper.selectList(mappingWrapper);
        if (mappings.isEmpty()) {
            return List.of();
        }

        List<Long> activityIds = mappings.stream().map(ActivityStore::getActivityId).distinct().toList();
        LambdaQueryWrapper<PromotionActivity> activityWrapper = new LambdaQueryWrapper<PromotionActivity>()
            .eq(PromotionActivity::getDeleted, 0)
            .eq(PromotionActivity::getStatus, PUBLISHED)
            .in(PromotionActivity::getId, activityIds)
            .orderByDesc(PromotionActivity::getStartAt)
            .orderByDesc(PromotionActivity::getId);
        if (type != null && !type.isBlank()) {
            activityWrapper.eq(PromotionActivity::getType, type.trim().toUpperCase());
        }
        List<PromotionActivity> activities = activityMapper.selectList(activityWrapper);
        if (activities.isEmpty()) {
            return List.of();
        }

        Set<Long> visibleActivityIds = activities.stream().map(PromotionActivity::getId).collect(Collectors.toSet());
        Map<Long, List<ActivityStore>> mappingsByActivity = mappings.stream()
            .filter(mapping -> visibleActivityIds.contains(mapping.getActivityId()))
            .collect(Collectors.groupingBy(ActivityStore::getActivityId, LinkedHashMap::new, Collectors.toList()));
        List<Long> storeIds = mappings.stream().map(ActivityStore::getStoreId).distinct().toList();
        Map<Long, Store> storeMap = storeIds.isEmpty()
            ? Map.of()
            : storeMapper.selectBatchIds(storeIds).stream().collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left));

        return activities.stream()
            .map(activity -> toActivityResponse(activity, mappingsByActivity.getOrDefault(activity.getId(), List.of()), storeMap))
            .toList();
    }

    public PointsSummaryResponse points(Long userId) {
        ensureLogin(userId);
        MemberAccount account = loadOrCreateMemberAccount(userId);
        List<PointsTransactionRow> transactions = pointsTransactionMapper.selectList(new LambdaQueryWrapper<PointsTransaction>()
                .eq(PointsTransaction::getUserId, userId)
                .orderByDesc(PointsTransaction::getCreatedAt)
                .orderByDesc(PointsTransaction::getId))
            .stream()
            .map(this::toPointsRow)
            .toList();
        return new PointsSummaryResponse(
            account.getId(),
            account.getLevelCode(),
            account.getPoints() == null ? 0 : account.getPoints(),
            account.getTotalSpent() == null ? BigDecimal.ZERO : account.getTotalSpent(),
            transactions
        );
    }

    public List<RewardCatalogResponse> rewards(Long userId) {
        ensureLogin(userId);
        LocalDateTime now = LocalDateTime.now();
        return rewardCatalogMapper.selectList(new LambdaQueryWrapper<RewardCatalog>()
                .eq(RewardCatalog::getDeleted, 0)
                .eq(RewardCatalog::getStatus, ACTIVE)
                .and(wrapper -> wrapper.isNull(RewardCatalog::getValidFrom).or().le(RewardCatalog::getValidFrom, now))
                .and(wrapper -> wrapper.isNull(RewardCatalog::getValidTo).or().ge(RewardCatalog::getValidTo, now))
                .orderByAsc(RewardCatalog::getPointsCost)
                .orderByAsc(RewardCatalog::getId))
            .stream()
            .map(this::toRewardResponse)
            .toList();
    }

    @Transactional
    public RedeemRewardResponse redeemReward(Long userId, Long rewardId) {
        ensureLogin(userId);
        if (rewardId == null) {
            throw new BizException(3341, "请选择要兑换的奖励");
        }
        RewardCatalog reward = rewardCatalogMapper.selectById(rewardId);
        validateRewardAvailable(reward);

        MemberAccount account = loadOrCreateMemberAccount(userId);
        int cost = reward.getPointsCost() == null ? 0 : reward.getPointsCost();
        if (cost <= 0) {
            throw new BizException(3342, "奖励积分配置异常");
        }
        if (reward.getStock() != null && reward.getStock() <= 0) {
            throw new BizException(3343, "该奖励已兑完");
        }
        if (account.getPoints() == null || account.getPoints() < cost) {
            throw new BizException(3344, "积分不足，暂时不能兑换");
        }
        if (reward.getStock() != null && rewardCatalogMapper.decreaseStockIfAvailable(reward.getId()) == 0) {
            throw new BizException(3343, "该奖励已兑完");
        }
        if (memberAccountMapper.deductPointsIfEnough(account.getId(), cost) == 0) {
            throw new BizException(3344, "积分不足，暂时不能兑换");
        }

        MemberAccount updatedAccount = memberAccountMapper.selectById(account.getId());
        int balanceAfter = updatedAccount == null || updatedAccount.getPoints() == null ? 0 : updatedAccount.getPoints();

        RewardRedemption redemption = new RewardRedemption();
        redemption.setRedemptionNo(generateRedemptionNo(userId));
        redemption.setUserId(userId);
        redemption.setMemberAccountId(account.getId());
        redemption.setRewardId(reward.getId());
        redemption.setRewardName(reward.getName());
        redemption.setPointsCost(cost);
        redemption.setStatus(REDEEMED);
        redemption.setRedeemedAt(LocalDateTime.now());
        redemption.setDeleted(0);
        rewardRedemptionMapper.insert(redemption);

        PointsTransaction transaction = new PointsTransaction();
        transaction.setUserId(userId);
        transaction.setMemberAccountId(account.getId());
        transaction.setRewardRedemptionId(redemption.getId());
        transaction.setType(REDEEM);
        transaction.setPoints(-cost);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription("兑换 " + reward.getName());
        pointsTransactionMapper.insert(transaction);

        RewardCatalog latestReward = rewardCatalogMapper.selectById(reward.getId());
        RewardRedemption savedRedemption = rewardRedemptionMapper.selectById(redemption.getId());
        return new RedeemRewardResponse(
            toRedemptionResponse(savedRedemption == null ? redemption : savedRedemption),
            balanceAfter,
            latestReward == null ? toRewardResponse(reward) : toRewardResponse(latestReward)
        );
    }

    public List<RewardRedemptionResponse> myRedemptions(Long userId) {
        ensureLogin(userId);
        return rewardRedemptionMapper.selectList(new LambdaQueryWrapper<RewardRedemption>()
                .eq(RewardRedemption::getUserId, userId)
                .eq(RewardRedemption::getDeleted, 0)
                .orderByDesc(RewardRedemption::getRedeemedAt)
                .orderByDesc(RewardRedemption::getId))
            .stream()
            .map(this::toRedemptionResponse)
            .toList();
    }

    public List<PreferenceResponse> preferences(Long userId) {
        ensureLogin(userId);
        return userPreferenceMapper.selectList(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .orderByAsc(UserPreference::getPreferenceType)
                .orderByAsc(UserPreference::getId))
            .stream()
            .map(this::toPreferenceResponse)
            .toList();
    }

    @Transactional
    public List<PreferenceResponse> savePreferences(Long userId, List<PreferenceRequest> requests) {
        ensureLogin(userId);
        userPreferenceMapper.delete(new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId));
        for (PreferenceRequest request : normalizePreferences(requests)) {
            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setPreferenceType(request.preferenceType());
            preference.setPreferenceValue(request.preferenceValue());
            userPreferenceMapper.insert(preference);
        }
        return preferences(userId);
    }

    @Transactional
    public ReviewResponse createReview(Long userId, Long orderId, ReviewRequest request) {
        ensureLogin(userId);
        if (orderId == null) {
            throw new BizException(3301, "请选择要评价的订单");
        }
        if (request == null || request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new BizException(3302, "评分需为 1-5 分");
        }
        FoodOrder order = orderService.getOwnedOrder(userId, orderId);
        if (!COMPLETED.equals(order.getStatus())) {
            throw new BizException(3303, "订单完成后才能评价");
        }
        Long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
            .eq(Review::getUserId, userId)
            .eq(Review::getOrderId, orderId)
            .eq(Review::getDeleted, 0));
        if (count > 0) {
            throw new BizException(3304, "该订单已经评价过了");
        }
        Review review = new Review();
        review.setUserId(userId);
        review.setStoreId(order.getStoreId());
        review.setOrderId(order.getId());
        review.setReservationId(order.getReservationId());
        review.setRating(request.rating());
        review.setContent(limitText(normalizeOptional(request.content()), 1000));
        review.setStatus(VISIBLE);
        reviewMapper.insert(review);
        return toReviewResponse(review, order.getOrderNo(), null);
    }

    public List<ReviewResponse> myReviews(Long userId) {
        ensureLogin(userId);
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getDeleted, 0)
                .orderByDesc(Review::getCreatedAt)
                .orderByDesc(Review::getId))
            .stream()
            .map(review -> toReviewResponse(review, null, null))
            .toList();
    }

    @Transactional
    public RefundResponse applyRefund(Long userId, Long orderId, RefundRequestPayload request) {
        ensureLogin(userId);
        if (orderId == null) {
            throw new BizException(3311, "请选择要退款的订单");
        }
        FoodOrder order = orderService.getOwnedOrder(userId, orderId);
        if (!PAID.equals(order.getStatus()) && !PREPARING.equals(order.getStatus())) {
            throw new BizException(3312, "只有已支付或制作中的订单可以申请退款");
        }
        String currentRefundStatus = normalizeRefundStatus(order.getRefundStatus());
        if (!NONE.equals(currentRefundStatus)) {
            throw new BizException(3313, "该订单已有退款处理记录");
        }
        Long count = refundRequestMapper.selectCount(new LambdaQueryWrapper<RefundRequest>()
            .eq(RefundRequest::getUserId, userId)
            .eq(RefundRequest::getOrderId, orderId));
        if (count > 0) {
            throw new BizException(3313, "该订单已有退款处理记录");
        }

        RefundRequest refund = new RefundRequest();
        refund.setRefundNo(generateRefundNo(userId));
        refund.setUserId(userId);
        refund.setOrderId(order.getId());
        refund.setAmount(order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
        refund.setReason(limitText(normalizeOptional(request == null ? null : request.reason()), 500));
        refund.setStatus(APPLIED);
        refundRequestMapper.insert(refund);

        order.setRefundStatus(APPLIED);
        orderMapper.updateById(order);
        return toRefundResponse(refund, order.getOrderNo(), order.getStatus(), order.getStoreId());
    }

    public List<RefundResponse> myRefunds(Long userId) {
        ensureLogin(userId);
        return refundRequestMapper.selectList(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getUserId, userId)
                .orderByDesc(RefundRequest::getCreatedAt)
                .orderByDesc(RefundRequest::getId))
            .stream()
            .map(refund -> toRefundResponse(refund, null, null, null))
            .toList();
    }

    @Transactional
    public void awardPointsForPaidOrder(FoodOrder order) {
        if (order == null || order.getUserId() == null || order.getId() == null) {
            return;
        }
        long existing = pointsTransactionMapper.selectCount(new LambdaQueryWrapper<PointsTransaction>()
            .eq(PointsTransaction::getOrderId, order.getId())
            .eq(PointsTransaction::getType, EARN));
        if (existing > 0) {
            return;
        }
        BigDecimal amount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        int earned = amount.toBigInteger().intValue();
        if (earned <= 0) {
            return;
        }
        MemberAccount account = loadOrCreateMemberAccount(order.getUserId());
        int currentPoints = account.getPoints() == null ? 0 : account.getPoints();
        int balanceAfter = currentPoints + earned;
        account.setPoints(balanceAfter);
        account.setTotalSpent((account.getTotalSpent() == null ? BigDecimal.ZERO : account.getTotalSpent()).add(amount));
        memberAccountMapper.updateById(account);

        PointsTransaction transaction = new PointsTransaction();
        transaction.setUserId(order.getUserId());
        transaction.setMemberAccountId(account.getId());
        transaction.setOrderId(order.getId());
        transaction.setType(EARN);
        transaction.setPoints(earned);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription("订单 " + order.getOrderNo() + " 沙箱支付获得积分");
        pointsTransactionMapper.insert(transaction);
    }

    private void validateRewardAvailable(RewardCatalog reward) {
        if (reward == null || Objects.equals(reward.getDeleted(), 1)) {
            throw new BizException(3342, "奖励不存在或已下架");
        }
        if (!ACTIVE.equals(reward.getStatus())) {
            throw new BizException(3342, "奖励暂不可兑换");
        }
        LocalDateTime now = LocalDateTime.now();
        if (reward.getValidFrom() != null && reward.getValidFrom().isAfter(now)) {
            throw new BizException(3342, "奖励还未开始兑换");
        }
        if (reward.getValidTo() != null && reward.getValidTo().isBefore(now)) {
            throw new BizException(3342, "奖励已过期");
        }
    }

    private HomeOrderStats buildOrderStats(List<OrderService.OrderResponse> orders) {
        long pendingPayment = orders.stream().filter(order -> "CREATED".equals(order.status())).count();
        long paid = orders.stream().filter(order -> PAID.equals(order.status())).count();
        long preparing = orders.stream().filter(order -> PREPARING.equals(order.status())).count();
        long completed = orders.stream().filter(order -> COMPLETED.equals(order.status())).count();
        long refundable = orders.stream().filter(OrderService.OrderResponse::canRefund).count();
        long reviewable = orders.stream().filter(OrderService.OrderResponse::canReview).count();
        return new HomeOrderStats(pendingPayment, paid, preparing, completed, refundable, reviewable);
    }

    private MemberAccount loadOrCreateMemberAccount(Long userId) {
        MemberAccount account = memberAccountMapper.selectOne(new LambdaQueryWrapper<MemberAccount>()
            .eq(MemberAccount::getUserId, userId)
            .last("LIMIT 1"));
        if (account != null) {
            return account;
        }
        MemberAccount created = new MemberAccount();
        created.setUserId(userId);
        created.setLevelCode("NORMAL");
        created.setPoints(0);
        created.setTotalSpent(BigDecimal.ZERO);
        memberAccountMapper.insert(created);
        return created;
    }

    private CustomerActivityResponse toActivityResponse(PromotionActivity activity, List<ActivityStore> mappings, Map<Long, Store> storeMap) {
        List<ActivityStoreResponse> stores = mappings.stream()
            .map(mapping -> {
                Store store = storeMap.get(mapping.getStoreId());
                return new ActivityStoreResponse(
                    mapping.getStoreId(),
                    store == null ? "未知门店" : store.getName(),
                    store == null ? null : store.getCity(),
                    store == null ? null : store.getAddress()
                );
            })
            .toList();
        return new CustomerActivityResponse(
            activity.getId(),
            activity.getTitle(),
            activity.getType(),
            activityTypeText(activity.getType()),
            activity.getDescription(),
            activity.getCoverUrl(),
            activity.getStartAt(),
            activity.getEndAt(),
            stores
        );
    }

    private String activityTypeText(String type) {
        if ("PROMOTION".equals(type)) return "优惠活动";
        if ("ENTERTAINMENT".equals(type)) return "娱乐活动";
        return type == null || type.isBlank() ? "主题活动" : type;
    }

    private List<PreferenceRequest> normalizePreferences(List<PreferenceRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        List<PreferenceRequest> result = new ArrayList<>();
        for (PreferenceRequest request : requests) {
            if (request == null) continue;
            String type = normalizeOptional(request.preferenceType());
            String value = normalizeOptional(request.preferenceValue());
            if (type == null || value == null) continue;
            PreferenceRequest normalized = new PreferenceRequest(type.toUpperCase(), limitText(value, 128));
            if (result.stream().noneMatch(item -> Objects.equals(item.preferenceType(), normalized.preferenceType())
                && Objects.equals(item.preferenceValue(), normalized.preferenceValue()))) {
                result.add(normalized);
            }
        }
        return result;
    }

    private PreferenceResponse toPreferenceResponse(UserPreference preference) {
        return new PreferenceResponse(preference.getId(), preference.getPreferenceType(), preference.getPreferenceValue());
    }

    private PointsTransactionRow toPointsRow(PointsTransaction transaction) {
        return new PointsTransactionRow(
            transaction.getId(),
            transaction.getOrderId(),
            transaction.getRewardRedemptionId(),
            transaction.getType(),
            transaction.getPoints(),
            transaction.getBalanceAfter(),
            transaction.getDescription(),
            transaction.getCreatedAt()
        );
    }

    private RewardCatalogResponse toRewardResponse(RewardCatalog reward) {
        return new RewardCatalogResponse(
            reward.getId(),
            reward.getName(),
            reward.getDescription(),
            reward.getPointsCost(),
            reward.getRewardType(),
            reward.getCoverUrl(),
            reward.getStock(),
            reward.getStatus(),
            reward.getValidFrom(),
            reward.getValidTo()
        );
    }

    private RewardRedemptionResponse toRedemptionResponse(RewardRedemption redemption) {
        return new RewardRedemptionResponse(
            redemption.getId(),
            redemption.getRedemptionNo(),
            redemption.getRewardId(),
            redemption.getRewardName(),
            redemption.getPointsCost(),
            redemption.getStatus(),
            redemption.getRedeemedAt(),
            redemption.getUsedAt()
        );
    }

    private ReviewResponse toReviewResponse(Review review, String orderNo, String storeName) {
        return new ReviewResponse(
            review.getId(),
            review.getOrderId(),
            orderNo,
            review.getStoreId(),
            storeName,
            review.getRating(),
            review.getContent(),
            review.getStatus(),
            review.getCreatedAt()
        );
    }

    private RefundResponse toRefundResponse(RefundRequest refund, String orderNo, String orderStatus, Long storeId) {
        return new RefundResponse(
            refund.getId(),
            refund.getRefundNo(),
            refund.getOrderId(),
            orderNo,
            storeId,
            refund.getAmount(),
            refund.getReason(),
            refund.getStatus(),
            orderStatus,
            refund.getReviewRemark(),
            refund.getCreatedAt(),
            refund.getReviewedAt()
        );
    }

    private String normalizeRefundStatus(String status) {
        String normalized = normalizeOptional(status);
        return normalized == null ? NONE : normalized.toUpperCase();
    }

    private String generateRefundNo(Long userId) {
        return "R" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId;
    }

    private String generateRedemptionNo(Long userId) {
        return "D" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId;
    }

    private void ensureLogin(Long userId) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
    }

    private String normalizeOptional(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record HomeResponse(
        PointsSummaryResponse points,
        HomeOrderStats orderStats,
        List<OrderService.OrderResponse> recentOrders,
        List<CustomerActivityResponse> activities,
        List<PreferenceResponse> preferences
    ) {}

    public record HomeOrderStats(long pendingPayment, long paid, long preparing, long completed, long refundable, long reviewable) {}

    public record CustomerActivityResponse(
        Long id,
        String title,
        String type,
        String typeText,
        String description,
        String coverUrl,
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<ActivityStoreResponse> stores
    ) {}

    public record ActivityStoreResponse(Long storeId, String storeName, String city, String address) {}

    public record PointsSummaryResponse(
        Long memberAccountId,
        String levelCode,
        Integer points,
        BigDecimal totalSpent,
        List<PointsTransactionRow> transactions
    ) {}

    public record PointsTransactionRow(Long id, Long orderId, Long rewardRedemptionId, String type, Integer points, Integer balanceAfter, String description, LocalDateTime createdAt) {}

    public record RewardCatalogResponse(
        Long id,
        String name,
        String description,
        Integer pointsCost,
        String rewardType,
        String coverUrl,
        Integer stock,
        String status,
        LocalDateTime validFrom,
        LocalDateTime validTo
    ) {}

    public record RewardRedemptionResponse(
        Long id,
        String redemptionNo,
        Long rewardId,
        String rewardName,
        Integer pointsCost,
        String status,
        LocalDateTime redeemedAt,
        LocalDateTime usedAt
    ) {}

    public record RedeemRewardResponse(RewardRedemptionResponse redemption, Integer balanceAfter, RewardCatalogResponse reward) {}

    public record PreferenceRequest(String preferenceType, String preferenceValue) {}

    public record PreferenceResponse(Long id, String preferenceType, String preferenceValue) {}

    public record ReviewRequest(Integer rating, String content) {}

    public record ReviewResponse(Long id, Long orderId, String orderNo, Long storeId, String storeName, Integer rating, String content, String status, LocalDateTime createdAt) {}

    public record RefundRequestPayload(String reason) {}

    public record RefundResponse(Long id, String refundNo, Long orderId, String orderNo, Long storeId, BigDecimal amount, String reason, String status, String orderStatus, String reviewRemark, LocalDateTime createdAt, LocalDateTime reviewedAt) {}
}
