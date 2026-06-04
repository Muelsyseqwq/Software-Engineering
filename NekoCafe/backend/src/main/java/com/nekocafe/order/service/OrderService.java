package com.nekocafe.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.menu.entity.Dish;
import com.nekocafe.menu.mapper.DishMapper;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.entity.FoodOrderItem;
import com.nekocafe.order.mapper.FoodOrderItemMapper;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.reservation.entity.Reservation;
import com.nekocafe.reservation.mapper.ReservationMapper;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.StoreMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final String ON_SHELF = "ON_SHELF";
    private static final String CREATED = "CREATED";
    private static final String PAID = "PAID";

    private final FoodOrderMapper orderMapper;
    private final FoodOrderItemMapper orderItemMapper;
    private final DishMapper dishMapper;
    private final StoreMapper storeMapper;
    private final ReservationMapper reservationMapper;

    public OrderService(
        FoodOrderMapper orderMapper,
        FoodOrderItemMapper orderItemMapper,
        DishMapper dishMapper,
        StoreMapper storeMapper,
        ReservationMapper reservationMapper
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.dishMapper = dishMapper;
        this.storeMapper = storeMapper;
        this.reservationMapper = reservationMapper;
    }

    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        validateCreateRequest(request);
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
            .eq(Store::getId, request.storeId())
            .eq(Store::getDeleted, 0)
            .last("LIMIT 1"));
        if (store == null) {
            throw new BizException(3002, "门店不存在");
        }
        if (request.reservationId() != null) {
            Reservation reservation = reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getId, request.reservationId())
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getStoreId, request.storeId())
                .eq(Reservation::getDeleted, 0)
                .last("LIMIT 1"));
            if (reservation == null) {
                throw new BizException(3003, "预约记录不存在或不属于当前账号");
            }
        }

        List<Long> dishIds = request.items().stream().map(CreateOrderItemRequest::dishId).distinct().toList();
        Map<Long, Dish> dishMap = dishMapper.selectBatchIds(dishIds).stream()
            .filter(dish -> dish.getDeleted() == null || dish.getDeleted() == 0)
            .collect(Collectors.toMap(Dish::getId, dish -> dish));
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderItemRequest item : request.items()) {
            Dish dish = dishMap.get(item.dishId());
            if (dish == null || !request.storeId().equals(dish.getStoreId()) || !ON_SHELF.equals(dish.getStatus())) {
                throw new BizException(3004, "存在不可点选的菜品");
            }
            if (dish.getStock() != null && dish.getStock() < item.quantity()) {
                throw new BizException(3005, dish.getName() + "库存不足");
            }
            total = total.add(dish.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        FoodOrder order = new FoodOrder();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setStoreId(request.storeId());
        order.setReservationId(request.reservationId());
        order.setTotalAmount(total);
        order.setStatus(CREATED);
        order.setRemark(normalizeOptional(request.remark()));
        orderMapper.insert(order);

        for (CreateOrderItemRequest item : request.items()) {
            Dish dish = dishMap.get(item.dishId());
            FoodOrderItem orderItem = new FoodOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setDishId(dish.getId());
            orderItem.setDishName(dish.getName());
            orderItem.setUnitPrice(dish.getPrice());
            orderItem.setQuantity(item.quantity());
            orderItem.setSubtotal(dish.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
            orderItemMapper.insert(orderItem);
        }

        return toResponse(order, store, loadItems(order.getId()));
    }

    public List<OrderResponse> mine(Long userId) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return orderMapper.selectList(new LambdaQueryWrapper<FoodOrder>()
                .eq(FoodOrder::getUserId, userId)
                .eq(FoodOrder::getDeleted, 0)
                .orderByDesc(FoodOrder::getCreatedAt)
                .orderByDesc(FoodOrder::getId))
            .stream()
            .map(order -> toResponse(order, storeMapper.selectById(order.getStoreId()), loadItems(order.getId())))
            .toList();
    }

    public FoodOrder getOwnedOrder(Long userId, Long orderId) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        FoodOrder order = orderMapper.selectOne(new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getId, orderId)
            .eq(FoodOrder::getUserId, userId)
            .eq(FoodOrder::getDeleted, 0)
            .last("LIMIT 1"));
        if (order == null) {
            throw new BizException(3006, "订单不存在");
        }
        return order;
    }

    @Transactional
    public OrderResponse markPaid(FoodOrder order) {
        order.setStatus(PAID);
        orderMapper.updateById(order);
        return toResponse(order, storeMapper.selectById(order.getStoreId()), loadItems(order.getId()));
    }

    private List<OrderItemResponse> loadItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<FoodOrderItem>()
                .eq(FoodOrderItem::getOrderId, orderId)
                .orderByAsc(FoodOrderItem::getId))
            .stream()
            .map(item -> new OrderItemResponse(item.getId(), item.getDishId(), item.getDishName(), item.getUnitPrice(), item.getQuantity(), item.getSubtotal()))
            .toList();
    }

    private OrderResponse toResponse(FoodOrder order, Store store, List<OrderItemResponse> items) {
        return new OrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getStoreId(),
            store == null ? "未知门店" : store.getName(),
            order.getReservationId(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getRemark(),
            order.getCreatedAt(),
            items
        );
    }

    private void validateCreateRequest(CreateOrderRequest request) {
        if (request == null || request.storeId() == null) {
            throw new BizException(3001, "请选择下单门店");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BizException(3007, "请选择至少一款菜品");
        }
        for (CreateOrderItemRequest item : request.items()) {
            if (item == null || item.dishId() == null || item.quantity() == null || item.quantity() <= 0) {
                throw new BizException(3008, "菜品和数量不能为空");
            }
        }
    }

    private String generateOrderNo(Long userId) {
        return "O" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId;
    }

    private String normalizeOptional(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record CreateOrderRequest(Long storeId, Long reservationId, List<CreateOrderItemRequest> items, String remark) {
    }

    public record CreateOrderItemRequest(Long dishId, Integer quantity) {
    }

    public record OrderItemResponse(Long id, Long dishId, String dishName, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal) {
    }

    public record OrderResponse(
        Long id,
        String orderNo,
        Long storeId,
        String storeName,
        Long reservationId,
        BigDecimal totalAmount,
        String status,
        String remark,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
    ) {
    }
}
