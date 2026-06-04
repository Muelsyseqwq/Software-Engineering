package com.nekocafe.menu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.menu.entity.Dish;
import com.nekocafe.menu.entity.DishCategory;
import com.nekocafe.menu.mapper.DishCategoryMapper;
import com.nekocafe.menu.mapper.DishMapper;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.StoreMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private static final String ACTIVE = "ACTIVE";
    private static final String ON_SHELF = "ON_SHELF";

    private final StoreMapper storeMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final DishMapper dishMapper;

    public MenuService(StoreMapper storeMapper, DishCategoryMapper dishCategoryMapper, DishMapper dishMapper) {
        this.storeMapper = storeMapper;
        this.dishCategoryMapper = dishCategoryMapper;
        this.dishMapper = dishMapper;
    }

    public List<MenuCategoryResponse> storeMenu(Long storeId) {
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
            .eq(Store::getId, storeId)
            .eq(Store::getDeleted, 0)
            .last("LIMIT 1"));
        if (store == null) {
            throw new BizException(2001, "门店不存在");
        }

        List<DishCategory> categories = dishCategoryMapper.selectList(new LambdaQueryWrapper<DishCategory>()
            .eq(DishCategory::getStoreId, storeId)
            .eq(DishCategory::getStatus, ACTIVE)
            .orderByAsc(DishCategory::getSortOrder)
            .orderByAsc(DishCategory::getId));
        if (categories.isEmpty()) {
            return List.of();
        }

        List<Long> categoryIds = categories.stream().map(DishCategory::getId).toList();
        Map<Long, List<DishResponse>> dishesByCategory = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStoreId, storeId)
                .in(Dish::getCategoryId, categoryIds)
                .eq(Dish::getDeleted, 0)
                .eq(Dish::getStatus, ON_SHELF)
                .orderByAsc(Dish::getCategoryId)
                .orderByAsc(Dish::getId))
            .stream()
            .map(this::toDish)
            .collect(Collectors.groupingBy(DishResponse::categoryId));

        return categories.stream()
            .map(category -> new MenuCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSortOrder(),
                dishesByCategory.getOrDefault(category.getId(), List.of())
            ))
            .toList();
    }

    private DishResponse toDish(Dish dish) {
        return new DishResponse(
            dish.getId(),
            dish.getCategoryId(),
            dish.getName(),
            dish.getPrice(),
            dish.getStock(),
            dish.getStatus(),
            dish.getDescription(),
            dish.getImageUrl()
        );
    }

    public record MenuCategoryResponse(Long id, String name, Integer sortOrder, List<DishResponse> dishes) {
    }

    public record DishResponse(
        Long id,
        Long categoryId,
        String name,
        BigDecimal price,
        Integer stock,
        String status,
        String description,
        String imageUrl
    ) {
    }
}
