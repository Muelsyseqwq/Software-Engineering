package com.nekocafe.order;

import com.nekocafe.common.exception.BizException;
import com.nekocafe.customer.mapper.ReviewMapper;
import com.nekocafe.customer.service.RewardRedemptionService;
import com.nekocafe.menu.entity.Dish;
import com.nekocafe.menu.mapper.DishMapper;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.entity.FoodOrderItem;
import com.nekocafe.order.mapper.FoodOrderItemMapper;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.order.service.OrderService;
import com.nekocafe.reservation.entity.Reservation;
import com.nekocafe.reservation.mapper.ReservationMapper;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Test
    void createOrderSuccess() {
        FoodOrderMapper orderMapper = mock(FoodOrderMapper.class);
        FoodOrderItemMapper orderItemMapper = mock(FoodOrderItemMapper.class);
        DishMapper dishMapper = mock(DishMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        RewardRedemptionService rewardRedemptionService = mock(RewardRedemptionService.class);
        OrderService orderService = new OrderService(
            orderMapper, orderItemMapper, dishMapper, storeMapper,
            diningTableMapper, reservationMapper, reviewMapper, rewardRedemptionService);

        Store store = store(1L, "Test", "OPEN");
        Dish dish1 = dish(1L, 1L, new BigDecimal("25.00"), 10);
        Dish dish2 = dish(2L, 1L, new BigDecimal("30.00"), 10);

        when(storeMapper.selectOne(any())).thenReturn(store);
        when(dishMapper.selectBatchIds(any())).thenReturn(List.of(dish1, dish2));
        when(rewardRedemptionService.prepareForOrder(any(), any(), any())).thenReturn(null);
        when(orderMapper.insert(any(FoodOrder.class))).thenAnswer(inv -> {
            FoodOrder o = inv.getArgument(0);
            o.setId(100L);
            return 1;
        });
        when(orderItemMapper.insert(any(FoodOrderItem.class))).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        when(reviewMapper.selectCount(any())).thenReturn(0L);

        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
            1L, null,
            List.of(
                new OrderService.CreateOrderItemRequest(1L, 1),
                new OrderService.CreateOrderItemRequest(2L, 1)
            ),
            null, null);

        OrderService.OrderResponse response = orderService.create(1L, request);

        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("55.00"));
    }

    @Test
    void createOrderDishInsufficientStockThrows() {
        FoodOrderMapper orderMapper = mock(FoodOrderMapper.class);
        FoodOrderItemMapper orderItemMapper = mock(FoodOrderItemMapper.class);
        DishMapper dishMapper = mock(DishMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        RewardRedemptionService rewardRedemptionService = mock(RewardRedemptionService.class);
        OrderService orderService = new OrderService(
            orderMapper, orderItemMapper, dishMapper, storeMapper,
            diningTableMapper, reservationMapper, reviewMapper, rewardRedemptionService);

        Store store = store(1L, "Test", "OPEN");
        Dish dish1 = dish(1L, 1L, new BigDecimal("25.00"), 10);
        Dish dish2 = dish(2L, 1L, new BigDecimal("30.00"), 0);

        when(storeMapper.selectOne(any())).thenReturn(store);
        when(dishMapper.selectBatchIds(any())).thenReturn(List.of(dish1, dish2));

        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
            1L, null,
            List.of(
                new OrderService.CreateOrderItemRequest(1L, 1),
                new OrderService.CreateOrderItemRequest(2L, 2)
            ),
            null, null);

        assertThatThrownBy(() -> orderService.create(1L, request))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("库存不足");
    }

    @Test
    void createOrderWithCouponDiscount() {
        FoodOrderMapper orderMapper = mock(FoodOrderMapper.class);
        FoodOrderItemMapper orderItemMapper = mock(FoodOrderItemMapper.class);
        DishMapper dishMapper = mock(DishMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        RewardRedemptionService rewardRedemptionService = mock(RewardRedemptionService.class);
        OrderService orderService = new OrderService(
            orderMapper, orderItemMapper, dishMapper, storeMapper,
            diningTableMapper, reservationMapper, reviewMapper, rewardRedemptionService);

        Store store = store(1L, "Test", "OPEN");
        Dish dish1 = dish(1L, 1L, new BigDecimal("25.00"), 10);
        Dish dish2 = dish(2L, 1L, new BigDecimal("30.00"), 10);
        RewardRedemptionService.CouponUsage couponUsage =
            new RewardRedemptionService.CouponUsage(1L, "8元券", new BigDecimal("8"));

        when(storeMapper.selectOne(any())).thenReturn(store);
        when(dishMapper.selectBatchIds(any())).thenReturn(List.of(dish1, dish2));
        when(rewardRedemptionService.prepareForOrder(any(), any(), any())).thenReturn(couponUsage);
        when(orderMapper.insert(any(FoodOrder.class))).thenAnswer(inv -> {
            FoodOrder o = inv.getArgument(0);
            o.setId(100L);
            return 1;
        });
        when(orderItemMapper.insert(any(FoodOrderItem.class))).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        when(reviewMapper.selectCount(any())).thenReturn(0L);

        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
            1L, null,
            List.of(
                new OrderService.CreateOrderItemRequest(1L, 1),
                new OrderService.CreateOrderItemRequest(2L, 1)
            ),
            null, 1L);

        OrderService.OrderResponse response = orderService.create(1L, request);

        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("55.00"));
        assertThat(response.payableAmount()).isEqualByComparingTo(new BigDecimal("47.00"));
    }

    @Test
    void cancelCreatedOrderSuccess() {
        FoodOrderMapper orderMapper = mock(FoodOrderMapper.class);
        FoodOrderItemMapper orderItemMapper = mock(FoodOrderItemMapper.class);
        DishMapper dishMapper = mock(DishMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        RewardRedemptionService rewardRedemptionService = mock(RewardRedemptionService.class);
        OrderService orderService = new OrderService(
            orderMapper, orderItemMapper, dishMapper, storeMapper,
            diningTableMapper, reservationMapper, reviewMapper, rewardRedemptionService);

        FoodOrder order = foodOrder(1L, 1L, 1L, "CREATED");
        Store store = store(1L, "Test", "OPEN");

        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.updateById(any(FoodOrder.class))).thenReturn(1);
        when(storeMapper.selectById(1L)).thenReturn(store);
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        when(reviewMapper.selectCount(any())).thenReturn(0L);

        OrderService.OrderResponse response = orderService.cancelOrder(1L, 1L);

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(response.canCancel()).isFalse();
        assertThat(response.cancelledAt()).isNotNull();
    }

    @Test
    void cancelPaidOrderFails() {
        FoodOrderMapper orderMapper = mock(FoodOrderMapper.class);
        FoodOrderItemMapper orderItemMapper = mock(FoodOrderItemMapper.class);
        DishMapper dishMapper = mock(DishMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        RewardRedemptionService rewardRedemptionService = mock(RewardRedemptionService.class);
        OrderService orderService = new OrderService(
            orderMapper, orderItemMapper, dishMapper, storeMapper,
            diningTableMapper, reservationMapper, reviewMapper, rewardRedemptionService);

        FoodOrder order = foodOrder(1L, 1L, 1L, "PAID");

        when(orderMapper.selectOne(any())).thenReturn(order);

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("只有待支付订单可以直接取消");
    }

    @Test
    void createSecondOrderForSameReservationFails() {
        FoodOrderMapper orderMapper = mock(FoodOrderMapper.class);
        FoodOrderItemMapper orderItemMapper = mock(FoodOrderItemMapper.class);
        DishMapper dishMapper = mock(DishMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        RewardRedemptionService rewardRedemptionService = mock(RewardRedemptionService.class);
        OrderService orderService = new OrderService(
            orderMapper, orderItemMapper, dishMapper, storeMapper,
            diningTableMapper, reservationMapper, reviewMapper, rewardRedemptionService);

        Store store = store(1L, "Test", "OPEN");
        Dish dish = dish(1L, 1L, new BigDecimal("25.00"), 10);
        Reservation reservation = reservation(1L, 1L, 1L, "RESERVED");

        when(storeMapper.selectOne(any())).thenReturn(store);
        when(reservationMapper.selectOne(any())).thenReturn(reservation);
        when(dishMapper.selectBatchIds(any())).thenReturn(List.of(dish));
        // Already has a CREATED order for this reservation
        when(orderMapper.selectCount(any())).thenReturn(1L);

        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
            1L, 1L, List.of(new OrderService.CreateOrderItemRequest(1L, 1)),
            null, null);

        assertThatThrownBy(() -> orderService.create(1L, request))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("已有一笔待支付订单");
    }

    // --- helpers ---

    private Store store(Long id, String name, String status) {
        Store store = new Store();
        store.setId(id);
        store.setName(name);
        store.setStatus(status);
        return store;
    }

    private Dish dish(Long id, Long storeId, BigDecimal price, Integer stock) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setStoreId(storeId);
        dish.setName("Dish-" + id);
        dish.setPrice(price);
        dish.setStock(stock);
        dish.setStatus("ON_SHELF");
        return dish;
    }

    private FoodOrder foodOrder(Long id, Long userId, Long storeId, String status) {
        FoodOrder order = new FoodOrder();
        order.setId(id);
        order.setUserId(userId);
        order.setStoreId(storeId);
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPayableAmount(BigDecimal.ZERO);
        order.setCouponDiscountAmount(BigDecimal.ZERO);
        return order;
    }

    private Reservation reservation(Long id, Long userId, Long storeId, String status) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setUserId(userId);
        r.setStoreId(storeId);
        r.setTableId(1L);
        r.setSlotId(1L);
        r.setStatus(status);
        r.setDeleted(0);
        return r;
    }
}
