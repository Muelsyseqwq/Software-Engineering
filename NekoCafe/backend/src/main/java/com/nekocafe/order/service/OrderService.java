package com.nekocafe.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.customer.entity.Review;
import com.nekocafe.customer.mapper.ReviewMapper;
import com.nekocafe.customer.service.RewardRedemptionService;
import com.nekocafe.menu.entity.Dish;
import com.nekocafe.menu.mapper.DishMapper;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.entity.FoodOrderItem;
import com.nekocafe.order.mapper.FoodOrderItemMapper;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.queue.entity.WaitingQueueTicket;
import com.nekocafe.queue.mapper.WaitingQueueTicketMapper;
import com.nekocafe.reservation.entity.Reservation;
import com.nekocafe.reservation.mapper.ReservationMapper;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
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
    private static final String PREPARING = "PREPARING";
    private static final String COMPLETED = "COMPLETED";
    private static final String NONE = "NONE";
    private static final String CANCELLED = "CANCELLED";
    private static final String RESERVED_STATUS = "RESERVED";
    private static final String CHECKED_IN = "CHECKED_IN";
    private static final String SEATED = "SEATED";

    private final FoodOrderMapper orderMapper;
    private final FoodOrderItemMapper orderItemMapper;
    private final DishMapper dishMapper;
    private final StoreMapper storeMapper;
    private final DiningTableMapper diningTableMapper;
    private final ReservationMapper reservationMapper;
    private final WaitingQueueTicketMapper waitingQueueTicketMapper;
    private final ReviewMapper reviewMapper;
    private final RewardRedemptionService rewardRedemptionService;

    public OrderService(
        FoodOrderMapper orderMapper,
        FoodOrderItemMapper orderItemMapper,
        DishMapper dishMapper,
        StoreMapper storeMapper,
        DiningTableMapper diningTableMapper,
        ReservationMapper reservationMapper,
        WaitingQueueTicketMapper waitingQueueTicketMapper,
        ReviewMapper reviewMapper,
        RewardRedemptionService rewardRedemptionService
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.dishMapper = dishMapper;
        this.storeMapper = storeMapper;
        this.diningTableMapper = diningTableMapper;
        this.reservationMapper = reservationMapper;
        this.waitingQueueTicketMapper = waitingQueueTicketMapper;
        this.reviewMapper = reviewMapper;
        this.rewardRedemptionService = rewardRedemptionService;
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
        Reservation reservation = null;
        WaitingQueueTicket queueTicket = null;
        if (request.reservationId() != null) {
            reservation = reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getId, request.reservationId())
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getStoreId, request.storeId())
                .eq(Reservation::getDeleted, 0)
                .last("LIMIT 1"));
            if (reservation == null) {
                throw new BizException(3003, "预约记录不存在或不属于当前账号");
            }
        }

        validateReservationCanBeUsedForOrder(reservation);
        if (request.queueTicketId() != null) {
            queueTicket = loadQueueTicketForOrder(userId, request);
        }
        if (reservation != null) {
            long existingCreatedCount = orderMapper.selectCount(new LambdaQueryWrapper<FoodOrder>()
                .eq(FoodOrder::getUserId, userId)
                .eq(FoodOrder::getReservationId, reservation.getId())
                .eq(FoodOrder::getDeleted, 0)
                .eq(FoodOrder::getStatus, CREATED));
            if (existingCreatedCount > 0) {
                throw new BizException(3012, "该预约已有一笔待支付订单，请先完成支付或取消后再下单");
            }
        }
        if (queueTicket != null) {
            long existingCreatedCount = orderMapper.selectCount(new LambdaQueryWrapper<FoodOrder>()
                .eq(FoodOrder::getUserId, userId)
                .eq(FoodOrder::getQueueTicketId, queueTicket.getId())
                .eq(FoodOrder::getDeleted, 0)
                .eq(FoodOrder::getStatus, CREATED));
            if (existingCreatedCount > 0) {
                throw new BizException(3013, "该排队入座已有一笔待支付订单，请先完成支付或取消后再下单");
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

        RewardRedemptionService.CouponUsage couponUsage = rewardRedemptionService.prepareForOrder(userId, request.rewardRedemptionId(), total);
        BigDecimal discountAmount = couponUsage == null ? BigDecimal.ZERO : couponUsage.discountAmount();
        BigDecimal payableAmount = total.subtract(discountAmount).max(BigDecimal.ZERO);

        FoodOrder order = new FoodOrder();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setStoreId(request.storeId());
        order.setReservationId(request.reservationId());
        order.setQueueTicketId(queueTicket == null ? null : queueTicket.getId());
        order.setTableId(queueTicket != null ? queueTicket.getTableId() : (reservation == null ? null : reservation.getTableId()));
        order.setTotalAmount(total);
        order.setRewardRedemptionId(couponUsage == null ? null : couponUsage.redemptionId());
        order.setCouponDiscountAmount(discountAmount);
        order.setPayableAmount(payableAmount);
        order.setCouponName(couponUsage == null ? null : couponUsage.rewardName());
        order.setStatus(CREATED);
        order.setRefundStatus(NONE);
        order.setRemark(normalizeOptional(request.remark()));
        orderMapper.insert(order);
        rewardRedemptionService.bindToOrder(userId, couponUsage, order.getId());

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
        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
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
        Review review = loadReview(order.getUserId(), order.getId());
        boolean reviewed = review != null;
        String refundStatus = normalizeRefundStatus(order.getRefundStatus());
        DiningTable table = order.getTableId() == null ? null : diningTableMapper.selectById(order.getTableId());
        return new OrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getStoreId(),
            store == null ? "未知门店" : store.getName(),
            order.getTableId(),
            table == null ? null : table.getTableNo(),
            order.getReservationId(),
            order.getQueueTicketId(),
            order.getTotalAmount(),
            order.getRewardRedemptionId(),
            order.getCouponName(),
            order.getCouponDiscountAmount() == null ? BigDecimal.ZERO : order.getCouponDiscountAmount(),
            order.getPayableAmount() == null ? order.getTotalAmount() : order.getPayableAmount(),
            order.getStatus(),
            refundStatus,
            order.getRemark(),
            order.getPaidAt(),
            order.getCompletedAt(),
            order.getCancelledAt(),
            order.getCreatedAt(),
            CREATED.equals(order.getStatus()),
            (PAID.equals(order.getStatus()) || PREPARING.equals(order.getStatus())) && NONE.equals(refundStatus),
            COMPLETED.equals(order.getStatus()) && !reviewed,
            reviewed,
            review == null ? null : review.getRating(),
            review == null ? null : review.getContent(),
            review == null ? null : review.getCreatedAt(),
            CREATED.equals(order.getStatus()),
            items
        );
    }

    private Review loadReview(Long userId, Long orderId) {
        if (userId == null || orderId == null) {
            return null;
        }
        return reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
            .eq(Review::getUserId, userId)
            .eq(Review::getOrderId, orderId)
            .eq(Review::getDeleted, 0)
            .orderByDesc(Review::getCreatedAt)
            .last("LIMIT 1"));
    }

    private WaitingQueueTicket loadQueueTicketForOrder(Long userId, CreateOrderRequest request) {
        WaitingQueueTicket ticket = waitingQueueTicketMapper.selectOne(new LambdaQueryWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getId, request.queueTicketId())
            .eq(WaitingQueueTicket::getUserId, userId)
            .eq(WaitingQueueTicket::getStoreId, request.storeId())
            .eq(WaitingQueueTicket::getDeleted, 0)
            .last("LIMIT 1"));
        if (ticket == null) {
            throw new BizException(3014, "排队入座记录不存在或不属于当前账号");
        }
        if (!SEATED.equals(ticket.getStatus()) || ticket.getTableId() == null) {
            throw new BizException(3015, "排队顾客确认入座后才可以点餐");
        }
        return ticket;
    }

    private void validateReservationCanBeUsedForOrder(Reservation reservation) {
        if (reservation == null) {
            return;
        }
        if (!List.of(RESERVED_STATUS, CHECKED_IN).contains(reservation.getStatus())) {
            throw new BizException(3009, "当前预约状态不可下单");
        }
    }

    public void validateReservationCanBePaid(FoodOrder order) {
        if (order == null || order.getReservationId() == null) {
            return;
        }
        Reservation reservation = reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
            .eq(Reservation::getId, order.getReservationId())
            .eq(Reservation::getUserId, order.getUserId())
            .eq(Reservation::getStoreId, order.getStoreId())
            .eq(Reservation::getDeleted, 0)
            .last("LIMIT 1"));
        if (reservation == null || !List.of(RESERVED_STATUS, CHECKED_IN).contains(reservation.getStatus())) {
            throw new BizException(3010, "关联预约已不可支付");
        }
    }

    public boolean hasPaidOrActiveOrdersForReservation(Long userId, Long reservationId) {
        if (userId == null || reservationId == null) {
            return false;
        }
        return orderMapper.selectCount(new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getUserId, userId)
            .eq(FoodOrder::getReservationId, reservationId)
            .eq(FoodOrder::getDeleted, 0)
            .in(FoodOrder::getStatus, List.of(PAID, PREPARING, COMPLETED))) > 0;
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        FoodOrder order = getOwnedOrder(userId, orderId);
        if (!CREATED.equals(order.getStatus())) {
            throw new BizException(3011, "只有待支付订单可以直接取消，已支付订单请申请退款");
        }
        rewardRedemptionService.releaseLockedForOrder(order);
        order.setStatus(CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return toResponse(order, storeMapper.selectById(order.getStoreId()), loadItems(order.getId()));
    }

    @Transactional
    public void cancelCreatedOrdersForReservation(Long userId, Long reservationId) {
        if (userId == null || reservationId == null) {
            return;
        }
        List<FoodOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getUserId, userId)
            .eq(FoodOrder::getReservationId, reservationId)
            .eq(FoodOrder::getDeleted, 0)
            .eq(FoodOrder::getStatus, CREATED));
        for (FoodOrder order : orders) {
            rewardRedemptionService.releaseLockedForOrder(order);
            order.setStatus(CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    private String normalizeRefundStatus(String status) {
        String normalized = normalizeOptional(status);
        return normalized == null ? NONE : normalized.toUpperCase();
    }

    private void validateCreateRequest(CreateOrderRequest request) {
        if (request == null || request.storeId() == null) {
            throw new BizException(3001, "请选择下单门店");
        }
        if (request.reservationId() != null && request.queueTicketId() != null) {
            throw new BizException(3016, "预约点餐和排队点餐不能同时关联");
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

    public record CreateOrderRequest(Long storeId, Long reservationId, Long queueTicketId, List<CreateOrderItemRequest> items, String remark, Long rewardRedemptionId) {
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
        Long tableId,
        String tableNo,
        Long reservationId,
        Long queueTicketId,
        BigDecimal totalAmount,
        Long rewardRedemptionId,
        String couponName,
        BigDecimal couponDiscountAmount,
        BigDecimal payableAmount,
        String status,
        String refundStatus,
        String remark,
        LocalDateTime paidAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        boolean canPay,
        boolean canRefund,
        boolean canReview,
        boolean reviewed,
        Integer reviewRating,
        String reviewContent,
        LocalDateTime reviewCreatedAt,
        boolean canCancel,
        List<OrderItemResponse> items
    ) {
    }
}
