package com.nekocafe.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.activity.entity.ActivityStore;
import com.nekocafe.activity.entity.PromotionActivity;
import com.nekocafe.activity.mapper.ActivityStoreMapper;
import com.nekocafe.activity.mapper.PromotionActivityMapper;
import com.nekocafe.cat.entity.Cat;
import com.nekocafe.cat.mapper.CatMapper;
import com.nekocafe.customer.entity.Review;
import com.nekocafe.customer.entity.UserPreference;
import com.nekocafe.customer.mapper.ReviewMapper;
import com.nekocafe.customer.mapper.UserPreferenceMapper;
import com.nekocafe.menu.entity.Dish;
import com.nekocafe.menu.entity.DishCategory;
import com.nekocafe.menu.mapper.DishCategoryMapper;
import com.nekocafe.menu.mapper.DishMapper;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.entity.FoodOrderItem;
import com.nekocafe.order.mapper.FoodOrderItemMapper;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.recommend.dto.RecommendationFeedResponse;
import com.nekocafe.recommend.dto.RecommendationHighlight;
import com.nekocafe.recommend.dto.RecommendationStoreItem;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.StoreMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendService {

    private static final String OPEN = "OPEN";
    private static final String ON_SHELF = "ON_SHELF";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String COMPLETED = "COMPLETED";
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final StoreMapper storeMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final DishMapper dishMapper;
    private final CatMapper catMapper;
    private final PromotionActivityMapper activityMapper;
    private final ActivityStoreMapper activityStoreMapper;
    private final FoodOrderMapper orderMapper;
    private final FoodOrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;

    public RecommendService(
        StoreMapper storeMapper,
        UserPreferenceMapper userPreferenceMapper,
        DishCategoryMapper dishCategoryMapper,
        DishMapper dishMapper,
        CatMapper catMapper,
        PromotionActivityMapper activityMapper,
        ActivityStoreMapper activityStoreMapper,
        FoodOrderMapper orderMapper,
        FoodOrderItemMapper orderItemMapper,
        ReviewMapper reviewMapper
    ) {
        this.storeMapper = storeMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.dishCategoryMapper = dishCategoryMapper;
        this.dishMapper = dishMapper;
        this.catMapper = catMapper;
        this.activityMapper = activityMapper;
        this.activityStoreMapper = activityStoreMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.reviewMapper = reviewMapper;
    }

    public RecommendationFeedResponse customerRecommendations(Long userId, BigDecimal latitude, BigDecimal longitude, Integer limit) {
        int max = Math.min(Math.max(limit == null ? 6 : limit, 1), 12);
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
            .eq(Store::getDeleted, 0)
            .eq(Store::getStatus, OPEN)
            .orderByAsc(Store::getId));
        if (stores.isEmpty()) {
            return new RecommendationFeedResponse(LocalDateTime.now(), "暂时没有可推荐的门店，稍后再来看看吧。", List.of());
        }

        List<Long> storeIds = stores.stream().map(Store::getId).toList();
        List<String> preferences = loadPreferences(userId);
        List<Long> preferredStoreIds = loadPreferredStoreIds(userId);
        Set<String> historyKeywords = loadHistoryKeywords(userId);
        Map<Long, List<DishCategory>> categoriesByStore = loadCategoriesByStore(storeIds);
        Map<Long, List<Dish>> dishesByStore = loadDishesByStore(storeIds);
        Map<Long, List<Cat>> catsByStore = loadCatsByStore(storeIds);
        Map<Long, List<PromotionActivity>> activitiesByStore = loadActivitiesByStore(storeIds);
        Map<Long, Double> reviewScores = loadReviewScores(storeIds);

        List<ScoredStore> scored = stores.stream()
            .map(store -> scoreStore(store, latitude, longitude, preferences, preferredStoreIds, historyKeywords,
                categoriesByStore.getOrDefault(store.getId(), List.of()),
                dishesByStore.getOrDefault(store.getId(), List.of()),
                catsByStore.getOrDefault(store.getId(), List.of()),
                activitiesByStore.getOrDefault(store.getId(), List.of()),
                reviewScores.getOrDefault(store.getId(), 0.0)))
            .sorted(Comparator.comparingInt(ScoredStore::score).reversed()
                .thenComparing(scoredStore -> scoredStore.distanceKm() == null ? Double.MAX_VALUE : scoredStore.distanceKm())
                .thenComparing(scoredStore -> scoredStore.store().getId()))
            .limit(max)
            .toList();

        List<RecommendationStoreItem> items = new ArrayList<>();
        for (int i = 0; i < scored.size(); i++) {
            items.add(toItem(i + 1, scored.get(i)));
        }
        String summary = buildSummary(items, preferences, latitude != null && longitude != null);
        return new RecommendationFeedResponse(LocalDateTime.now(), summary, items);
    }

    private ScoredStore scoreStore(
        Store store,
        BigDecimal latitude,
        BigDecimal longitude,
        List<String> preferences,
        List<Long> preferredStoreIds,
        Set<String> historyKeywords,
        List<DishCategory> categories,
        List<Dish> dishes,
        List<Cat> cats,
        List<PromotionActivity> activities,
        double reviewScore
    ) {
        int score = 0;
        Set<String> tags = new LinkedHashSet<>();
        List<String> reasons = new ArrayList<>();
        Double distanceKm = calculateDistanceKm(latitude, longitude, store.getLatitude(), store.getLongitude());

        if (OPEN.equals(store.getStatus())) {
            score += 30;
            tags.add("营业中");
        } else {
            tags.add("即将开放");
            reasons.add("这家店当前处于筹备或休息状态，适合作为后续打卡备选。");
        }

        if (distanceKm != null) {
            if (distanceKm <= 5) {
                score += 24;
                tags.add("距离很近");
                reasons.add("它离你当前位置较近，适合临时安排一次轻松的猫咖体验。");
            } else if (distanceKm <= 15) {
                score += 16;
                tags.add("距离适中");
                reasons.add("它在可接受的出行距离内，可以结合活动或菜品一起安排。");
            } else {
                score += 6;
                tags.add("可计划前往");
            }
        }

        int preferenceMatches = countPreferenceMatches(store, categories, dishes, cats, activities, preferences);
        if (preferenceMatches > 0) {
            int bonus = Math.min(24, preferenceMatches * 6);
            score += bonus;
            tags.add("匹配偏好");
            reasons.add("根据你的会员偏好，这家店的菜品、猫咪或活动内容更容易命中你的兴趣。");
        }

        if (preferredStoreIds.contains(store.getId())) {
            score += 14;
            tags.add("历史到访");
            reasons.add("你过去在这家店有消费或评价记录，系统优先考虑你熟悉的体验场景。");
        }

        int historyMatches = countHistoryMatches(dishes, historyKeywords);
        if (historyMatches > 0) {
            score += Math.min(12, historyMatches * 4);
            tags.add("口味相近");
            reasons.add("这家店的部分菜品和你历史订单中的口味关键词相近，适合继续探索。");
        }

        if (!dishes.isEmpty()) {
            score += Math.min(16, dishes.size() * 3);
            tags.add("菜品丰富");
            reasons.add("当前有多款上架菜品可选，适合点单搭配和多人同行。");
        }

        if (!cats.isEmpty()) {
            score += Math.min(18, cats.size() * 5);
            tags.add("猫咪可互动");
            reasons.add("门店有状态良好的猫咪可互动，适合安排更完整的猫咖体验。");
        }

        if (!activities.isEmpty()) {
            score += Math.min(14, activities.size() * 7);
            tags.add("活动进行中");
            reasons.add("门店当前有已发布活动，适合顺路打卡并参与优惠或娱乐体验。");
        }

        if (reviewScore > 0) {
            score += (int) Math.round(Math.min(10, reviewScore * 2));
            tags.add("评价较好");
        }

        List<RecommendationHighlight> dishHighlights = dishes.stream()
            .sorted(Comparator.comparing(Dish::getStock, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(3)
            .map(dish -> new RecommendationHighlight(dish.getId(), dish.getName(), formatDishDescription(dish)))
            .toList();
        List<RecommendationHighlight> catHighlights = cats.stream()
            .limit(3)
            .map(cat -> new RecommendationHighlight(cat.getId(), cat.getName(), formatCatDescription(cat)))
            .toList();
        List<RecommendationHighlight> activityHighlights = activities.stream()
            .limit(2)
            .map(activity -> new RecommendationHighlight(activity.getId(), activity.getTitle(), activity.getDescription()))
            .toList();

        List<String> finalReasons = reasons.stream().distinct().limit(4).toList();
        if (finalReasons.isEmpty()) {
            finalReasons = List.of("系统根据门店状态、菜品、猫咪和活动数据，为你生成了这条基础推荐。");
        }

        return new ScoredStore(store, score, distanceKm, List.copyOf(tags), finalReasons, dishHighlights, catHighlights, activityHighlights);
    }

    private RecommendationStoreItem toItem(int rank, ScoredStore scored) {
        Store store = scored.store();
        BigDecimal distance = scored.distanceKm() == null
            ? null
            : BigDecimal.valueOf(scored.distanceKm()).setScale(2, RoundingMode.HALF_UP);
        return new RecommendationStoreItem(
            rank,
            store.getId(),
            store.getName(),
            store.getCity(),
            store.getBusinessArea(),
            store.getAddress(),
            store.getStatus(),
            distance,
            scored.score(),
            scored.tags(),
            scored.reasons(),
            scored.dishHighlights(),
            scored.catHighlights(),
            scored.activityHighlights(),
            OPEN.equals(store.getStatus()) ? "去预约" : "查看门店"
        );
    }

    private List<String> loadPreferences(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userPreferenceMapper.selectList(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .orderByDesc(UserPreference::getUpdatedAt)
                .orderByDesc(UserPreference::getId))
            .stream()
            .map(UserPreference::getPreferenceValue)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    private List<Long> loadPreferredStoreIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        orderMapper.selectList(new LambdaQueryWrapper<FoodOrder>()
                .eq(FoodOrder::getUserId, userId)
                .eq(FoodOrder::getDeleted, 0)
                .in(FoodOrder::getStatus, List.of(COMPLETED, "PAID", "PREPARING"))
                .orderByDesc(FoodOrder::getCreatedAt)
                .last("LIMIT 12"))
            .forEach(order -> ids.add(order.getStoreId()));
        reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getDeleted, 0)
                .eq(Review::getStatus, "VISIBLE")
                .orderByDesc(Review::getCreatedAt)
                .last("LIMIT 8"))
            .forEach(review -> ids.add(review.getStoreId()));
        return ids.stream().filter(Objects::nonNull).toList();
    }

    private Set<String> loadHistoryKeywords(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        List<FoodOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getUserId, userId)
            .eq(FoodOrder::getDeleted, 0)
            .orderByDesc(FoodOrder::getCreatedAt)
            .last("LIMIT 10"));
        if (orders.isEmpty()) {
            return Set.of();
        }
        List<Long> orderIds = orders.stream().map(FoodOrder::getId).toList();
        return orderItemMapper.selectList(new LambdaQueryWrapper<FoodOrderItem>()
                .in(FoodOrderItem::getOrderId, orderIds))
            .stream()
            .map(FoodOrderItem::getDishName)
            .filter(Objects::nonNull)
            .flatMap(name -> tokenize(name).stream())
            .collect(Collectors.toSet());
    }

    private Map<Long, List<DishCategory>> loadCategoriesByStore(List<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        return dishCategoryMapper.selectList(new LambdaQueryWrapper<DishCategory>()
                .in(DishCategory::getStoreId, storeIds)
                .eq(DishCategory::getStatus, "ACTIVE")
                .orderByAsc(DishCategory::getSortOrder)
                .orderByAsc(DishCategory::getId))
            .stream()
            .collect(Collectors.groupingBy(DishCategory::getStoreId));
    }

    private Map<Long, List<Dish>> loadDishesByStore(List<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        return dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .in(Dish::getStoreId, storeIds)
                .eq(Dish::getDeleted, 0)
                .eq(Dish::getStatus, ON_SHELF)
                .gt(Dish::getStock, 0)
                .orderByDesc(Dish::getStock)
                .orderByAsc(Dish::getId))
            .stream()
            .collect(Collectors.groupingBy(Dish::getStoreId));
    }

    private Map<Long, List<Cat>> loadCatsByStore(List<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        return catMapper.selectList(new LambdaQueryWrapper<Cat>()
                .in(Cat::getStoreId, storeIds)
                .eq(Cat::getDeleted, 0)
                .eq(Cat::getStatus, AVAILABLE)
                .orderByAsc(Cat::getId))
            .stream()
            .collect(Collectors.groupingBy(Cat::getStoreId));
    }

    private Map<Long, List<PromotionActivity>> loadActivitiesByStore(List<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        List<ActivityStore> mappings = activityStoreMapper.selectList(new LambdaQueryWrapper<ActivityStore>()
            .in(ActivityStore::getStoreId, storeIds)
            .eq(ActivityStore::getAcceptStatus, ACCEPTED));
        if (mappings.isEmpty()) {
            return Map.of();
        }
        List<Long> activityIds = mappings.stream().map(ActivityStore::getActivityId).distinct().toList();
        Map<Long, PromotionActivity> activities = activityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .in(PromotionActivity::getId, activityIds)
                .eq(PromotionActivity::getDeleted, 0)
                .eq(PromotionActivity::getStatus, PUBLISHED)
                .orderByDesc(PromotionActivity::getStartAt)
                .orderByDesc(PromotionActivity::getId))
            .stream()
            .collect(Collectors.toMap(PromotionActivity::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<PromotionActivity>> result = new HashMap<>();
        for (ActivityStore mapping : mappings) {
            PromotionActivity activity = activities.get(mapping.getActivityId());
            if (activity != null) {
                result.computeIfAbsent(mapping.getStoreId(), ignored -> new ArrayList<>()).add(activity);
            }
        }
        return result;
    }

    private Map<Long, Double> loadReviewScores(List<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Review>> reviewsByStore = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .in(Review::getStoreId, storeIds)
                .eq(Review::getDeleted, 0)
                .eq(Review::getStatus, "VISIBLE"))
            .stream()
            .collect(Collectors.groupingBy(Review::getStoreId));
        Map<Long, Double> result = new HashMap<>();
        for (Map.Entry<Long, List<Review>> entry : reviewsByStore.entrySet()) {
            double average = entry.getValue().stream()
                .map(Review::getRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
            result.put(entry.getKey(), average);
        }
        return result;
    }

    private int countPreferenceMatches(Store store, List<DishCategory> categories, List<Dish> dishes, List<Cat> cats,
                                       List<PromotionActivity> activities, List<String> preferences) {
        if (preferences.isEmpty()) {
            return 0;
        }
        String searchable = String.join(" ", List.of(
            safe(store.getName()), safe(store.getCity()), safe(store.getBusinessArea()), safe(store.getAddress()), safe(store.getDescription()),
            categories.stream().map(DishCategory::getName).filter(Objects::nonNull).collect(Collectors.joining(" ")),
            dishes.stream().map(dish -> safe(dish.getName()) + " " + safe(dish.getDescription())).collect(Collectors.joining(" ")),
            cats.stream().map(cat -> safe(cat.getName()) + " " + safe(cat.getPersonality()) + " " + safe(cat.getDescription())).collect(Collectors.joining(" ")),
            activities.stream().map(activity -> safe(activity.getTitle()) + " " + safe(activity.getDescription())).collect(Collectors.joining(" "))
        )).toLowerCase(Locale.ROOT);
        int matches = 0;
        for (String preference : preferences) {
            String normalized = preference.toLowerCase(Locale.ROOT);
            if (!normalized.isBlank() && searchable.contains(normalized)) {
                matches++;
            }
        }
        return matches;
    }

    private int countHistoryMatches(List<Dish> dishes, Set<String> historyKeywords) {
        if (historyKeywords.isEmpty()) {
            return 0;
        }
        Set<String> matched = new HashSet<>();
        for (Dish dish : dishes) {
            String text = (safe(dish.getName()) + " " + safe(dish.getDescription())).toLowerCase(Locale.ROOT);
            for (String keyword : historyKeywords) {
                if (text.contains(keyword)) {
                    matched.add(keyword);
                }
            }
        }
        return matched.size();
    }

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}\\s]+", " ").trim();
        if (!normalized.isBlank()) {
            tokens.addAll(List.of(normalized.split(" ")));
        }
        for (int i = 0; i < value.length() - 1; i += 2) {
            tokens.add(value.substring(i, Math.min(value.length(), i + 2)).toLowerCase(Locale.ROOT));
        }
        return tokens.stream().filter(token -> token.length() >= 2).distinct().limit(8).toList();
    }

    private Double calculateDistanceKm(BigDecimal lat, BigDecimal lng, BigDecimal storeLat, BigDecimal storeLng) {
        if (lat == null || lng == null || storeLat == null || storeLng == null) {
            return null;
        }
        double lat1 = Math.toRadians(lat.doubleValue());
        double lat2 = Math.toRadians(storeLat.doubleValue());
        double deltaLat = Math.toRadians(storeLat.doubleValue() - lat.doubleValue());
        double deltaLng = Math.toRadians(storeLng.doubleValue() - lng.doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private String buildSummary(List<RecommendationStoreItem> items, List<String> preferences, boolean withLocation) {
        if (items.isEmpty()) {
            return "暂时没有生成推荐，完善会员偏好或稍后再试。";
        }
        String locationText = withLocation ? "结合你的位置、" : "结合";
        String preferenceText = preferences.isEmpty() ? "近期门店数据" : "你的会员偏好";
        return locationText + preferenceText + "、菜品、猫咪和活动热度，为你生成了 " + items.size() + " 条智能推荐。";
    }

    private String formatDishDescription(Dish dish) {
        String price = dish.getPrice() == null ? "" : "¥" + dish.getPrice().setScale(2, RoundingMode.HALF_UP);
        String description = dish.getDescription() == null ? "" : dish.getDescription();
        return (price + " " + description).trim();
    }

    private String formatCatDescription(Cat cat) {
        List<String> parts = new ArrayList<>();
        if (cat.getBreed() != null) {
            parts.add(cat.getBreed());
        }
        if (cat.getPersonality() != null) {
            parts.add(cat.getPersonality());
        }
        return String.join(" · ", parts);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ScoredStore(
        Store store,
        int score,
        Double distanceKm,
        List<String> tags,
        List<String> reasons,
        List<RecommendationHighlight> dishHighlights,
        List<RecommendationHighlight> catHighlights,
        List<RecommendationHighlight> activityHighlights
    ) {
    }
}
