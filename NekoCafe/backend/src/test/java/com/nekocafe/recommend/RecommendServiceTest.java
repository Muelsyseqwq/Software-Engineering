package com.nekocafe.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.activity.mapper.ActivityStoreMapper;
import com.nekocafe.activity.mapper.PromotionActivityMapper;
import com.nekocafe.cat.mapper.CatMapper;
import com.nekocafe.customer.entity.UserPreference;
import com.nekocafe.customer.mapper.ReviewMapper;
import com.nekocafe.customer.mapper.UserPreferenceMapper;
import com.nekocafe.menu.mapper.DishCategoryMapper;
import com.nekocafe.menu.mapper.DishMapper;
import com.nekocafe.order.mapper.FoodOrderItemMapper;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.recommend.ai.RecommendationReasonGenerator;
import com.nekocafe.recommend.dto.RecommendationFeedResponse;
import com.nekocafe.recommend.dto.RecommendationStoreItem;
import com.nekocafe.recommend.service.RecommendService;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.StoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendServiceTest {

    private final StoreMapper storeMapper = mock(StoreMapper.class);
    private final UserPreferenceMapper userPreferenceMapper = mock(UserPreferenceMapper.class);
    private final DishCategoryMapper dishCategoryMapper = mock(DishCategoryMapper.class);
    private final DishMapper dishMapper = mock(DishMapper.class);
    private final CatMapper catMapper = mock(CatMapper.class);
    private final PromotionActivityMapper activityMapper = mock(PromotionActivityMapper.class);
    private final ActivityStoreMapper activityStoreMapper = mock(ActivityStoreMapper.class);
    private final FoodOrderMapper orderMapper = mock(FoodOrderMapper.class);
    private final FoodOrderItemMapper orderItemMapper = mock(FoodOrderItemMapper.class);
    private final ReviewMapper reviewMapper = mock(ReviewMapper.class);
    private final RecommendationReasonGenerator reasonGenerator = mock(RecommendationReasonGenerator.class);

    private final RecommendService service = new RecommendService(
        storeMapper, userPreferenceMapper, dishCategoryMapper, dishMapper,
        catMapper, activityMapper, activityStoreMapper, orderMapper,
        orderItemMapper, reviewMapper, reasonGenerator);

    private static final BigDecimal USER_LAT = new BigDecimal("30.2450000");
    private static final BigDecimal USER_LNG = new BigDecimal("120.1510000");

    @BeforeEach
    void setUpEmptyDefaults() {
        when(userPreferenceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(dishCategoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(dishMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(catMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(activityStoreMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(activityMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(reasonGenerator.enhanceReasons(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void storesSortedByProximityAndScore() {
        Store nearStore = store(1L, "附近猫咖", "OPEN", "30.2500000", "120.1600000");
        Store farStore = store(2L, "远方猫咖", "OPEN", "30.3000000", "120.3000000");

        when(storeMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(nearStore, farStore));

        RecommendationFeedResponse resp = service.customerRecommendations(
            1L, USER_LAT, USER_LNG, 5);

        List<RecommendationStoreItem> items = resp.items();
        assertThat(items).isNotEmpty();
        assertThat(items.get(0).rank()).isEqualTo(1);
        assertThat(items.get(0).storeName()).isEqualTo("附近猫咖");
        assertThat(items.get(1).rank()).isEqualTo(2);
        assertThat(items.get(1).storeName()).isEqualTo("远方猫咖");
    }

    @Test
    void aiFallbackTemplateReason() {
        Store store = store(1L, "测试猫咖", "OPEN", "30.2500000", "120.1550000");
        when(storeMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(store));

        // reasonGenerator.enhanceReasons returns identity (AI disabled / fallback to template)
        when(reasonGenerator.enhanceReasons(any())).thenAnswer(inv -> inv.getArgument(0));

        RecommendationFeedResponse resp = service.customerRecommendations(
            1L, USER_LAT, USER_LNG, 5);

        assertThat(resp.summary()).isNotEmpty();

        RecommendationStoreItem item = resp.items().get(0);
        assertThat(item.reasons()).isNotNull();
        assertThat(item.reasons()).isNotEmpty();
    }

    @Test
    void userPreferencesAffectRanking() {
        // Both stores at similar distances so preference matching is the tiebreaker.
        Store unmatchedStore = store(1L, "普通猫咖", "OPEN", "30.2480000", "120.1550000");
        Store matchedStore = store(2L, "猫咪互动主题馆", "OPEN", "30.2500000", "120.1530000");

        when(storeMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(unmatchedStore, matchedStore));

        UserPreference pref = userPreference(1L, "CATEGORY", "猫咪互动");
        when(userPreferenceMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(pref));

        RecommendationFeedResponse resp = service.customerRecommendations(
            1L, USER_LAT, USER_LNG, 5);

        List<RecommendationStoreItem> items = resp.items();
        assertThat(items).isNotEmpty();
        assertThat(items.get(0).rank()).isEqualTo(1);
        assertThat(items.get(0).storeName()).isEqualTo("猫咪互动主题馆");
    }

    // ---- helpers ----

    private static Store store(Long id, String name, String status, String lat, String lng) {
        Store s = new Store();
        s.setId(id);
        s.setName(name);
        s.setCity("杭州");
        s.setBusinessArea("西湖区");
        s.setAddress("测试路" + id + "号");
        s.setStatus(status);
        s.setLatitude(new BigDecimal(lat));
        s.setLongitude(new BigDecimal(lng));
        s.setOpeningTime(LocalTime.of(10, 0));
        s.setClosingTime(LocalTime.of(22, 0));
        s.setDeleted(0);
        return s;
    }

    private static UserPreference userPreference(Long userId, String type, String value) {
        UserPreference p = new UserPreference();
        p.setId(1L);
        p.setUserId(userId);
        p.setPreferenceType(type);
        p.setPreferenceValue(value);
        return p;
    }
}
