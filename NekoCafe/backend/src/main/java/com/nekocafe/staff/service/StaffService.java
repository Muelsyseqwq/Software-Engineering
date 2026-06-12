package com.nekocafe.staff.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.cat.entity.Cat;
import com.nekocafe.cat.mapper.CatMapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.entity.FoodOrderItem;
import com.nekocafe.order.mapper.FoodOrderItemMapper;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.reservation.entity.Reservation;
import com.nekocafe.reservation.entity.ReservationSlot;
import com.nekocafe.reservation.mapper.ReservationMapper;
import com.nekocafe.reservation.mapper.ReservationSlotMapper;
import com.nekocafe.customer.entity.Review;
import com.nekocafe.customer.mapper.ReviewMapper;
import com.nekocafe.staff.dto.StaffOrderRow;
import com.nekocafe.staff.dto.StaffReservationRow;
import com.nekocafe.staff.dto.StaffReviewRow;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.DiningTableStatusLog;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.DiningTableStatusLogMapper;
import com.nekocafe.store.entity.UserStoreRole;
import com.nekocafe.store.mapper.UserStoreRoleMapper;
import com.nekocafe.user.entity.User;
import com.nekocafe.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StaffService {

    private static final String CREATED = "CREATED";
    private static final String PAID = "PAID";
    private static final String PREPARING = "PREPARING";
    private static final String COMPLETED = "COMPLETED";

    private final ReservationMapper reservationMapper;
    private final ReservationSlotMapper reservationSlotMapper;
    private final FoodOrderMapper foodOrderMapper;
    private final FoodOrderItemMapper foodOrderItemMapper;
    private final DiningTableMapper diningTableMapper;
    private final DiningTableStatusLogMapper diningTableStatusLogMapper;
    private final CatMapper catMapper;
    private final UserStoreRoleMapper userStoreRoleMapper;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;

    public StaffService(ReservationMapper reservationMapper,
                        ReservationSlotMapper reservationSlotMapper,
                        FoodOrderMapper foodOrderMapper,
                        FoodOrderItemMapper foodOrderItemMapper,
                        DiningTableMapper diningTableMapper,
                        DiningTableStatusLogMapper diningTableStatusLogMapper,
                        CatMapper catMapper,
                        UserStoreRoleMapper userStoreRoleMapper,
                        ReviewMapper reviewMapper,
                        UserMapper userMapper) {
        this.reservationMapper = reservationMapper;
        this.reservationSlotMapper = reservationSlotMapper;
        this.foodOrderMapper = foodOrderMapper;
        this.foodOrderItemMapper = foodOrderItemMapper;
        this.diningTableMapper = diningTableMapper;
        this.diningTableStatusLogMapper = diningTableStatusLogMapper;
        this.catMapper = catMapper;
        this.userStoreRoleMapper = userStoreRoleMapper;
        this.reviewMapper = reviewMapper;
        this.userMapper = userMapper;
    }

    private List<Long> resolveStoreIds(Long staffId) {
        LambdaQueryWrapper<UserStoreRole> wrapper = new LambdaQueryWrapper<UserStoreRole>()
            .eq(UserStoreRole::getUserId, staffId)
            .eq(UserStoreRole::getStatus, "ACTIVE")
            .eq(UserStoreRole::getRoleCode, "STAFF");
        List<UserStoreRole> roles = userStoreRoleMapper.selectList(wrapper);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream().map(UserStoreRole::getStoreId).distinct().toList();
    }

    public List<StaffReservationRow> todayReservations(Long staffId) {
        List<Long> storeIds = resolveStoreIds(staffId);
        if (storeIds.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
            .eq(Reservation::getDeleted, 0)
            .in(Reservation::getStoreId, storeIds)
            .between(Reservation::getCreatedAt, todayStart, todayEnd)
            .orderByDesc(Reservation::getCreatedAt);

        List<Reservation> list = reservationMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> slotIds = list.stream().map(Reservation::getSlotId).filter(Objects::nonNull).distinct().toList();
        final Map<Long, ReservationSlot> slotMap;
        if (!slotIds.isEmpty()) {
            List<ReservationSlot> slots = reservationSlotMapper.selectBatchIds(slotIds);
            slotMap = slots.stream().collect(Collectors.toMap(ReservationSlot::getId, s -> s));
        } else {
            slotMap = Collections.emptyMap();
        }

        List<Long> tableIds = list.stream().map(Reservation::getTableId).filter(Objects::nonNull).distinct().toList();
        final Map<Long, DiningTable> tableMap;
        if (!tableIds.isEmpty()) {
            List<DiningTable> tables = diningTableMapper.selectBatchIds(tableIds);
            tableMap = tables.stream().collect(Collectors.toMap(DiningTable::getId, t -> t));
        } else {
            tableMap = Collections.emptyMap();
        }

        return list.stream().map(r -> {
            ReservationSlot slot = slotMap.get(r.getSlotId());
            String timeSlot = slot != null
                ? slot.getStartTime() + " - " + slot.getEndTime()
                : "";
            DiningTable table = tableMap.get(r.getTableId());
            String tableNo = table != null ? table.getTableNo() : "-";
            return new StaffReservationRow(
                r.getId(),
                r.getReservationNo(),
                r.getContactName(),
                r.getContactPhone(),
                r.getPartySize(),
                timeSlot,
                r.getRemark(),
                tableNo,
                translateReservationStatus(r.getStatus())
            );
        }).toList();
    }

    @Transactional
    public void checkInReservation(Long id, Long staffId) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null || reservation.getDeleted() == 1) {
            throw new BizException(4001, "预约不存在");
        }
        reservation.setStatus("CHECKED_IN");
        reservation.setCheckedInAt(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        Long tableId = reservation.getTableId();
        if (tableId != null) {
            DiningTable table = diningTableMapper.selectById(tableId);
            if (table != null && table.getDeleted() == 0) {
                String oldStatus = table.getStatus();
                table.setStatus("OCCUPIED");
                diningTableMapper.updateById(table);

                DiningTableStatusLog log = new DiningTableStatusLog();
                log.setTableId(tableId);
                log.setStoreId(table.getStoreId());
                log.setOldStatus(oldStatus);
                log.setNewStatus("OCCUPIED");
                log.setChangedBy(staffId);
                log.setReason("预约签到，顾客入座");
                diningTableStatusLogMapper.insert(log);
            }
        }
    }

    public List<StaffOrderRow> pendingOrders(Long staffId) {
        List<Long> storeIds = resolveStoreIds(staffId);
        if (storeIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<FoodOrder> wrapper = new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getDeleted, 0)
            .in(FoodOrder::getStoreId, storeIds)
            .in(FoodOrder::getStatus, List.of(CREATED, PAID, PREPARING))
            .orderByDesc(FoodOrder::getCreatedAt);

        List<FoodOrder> orders = foodOrderMapper.selectList(wrapper);
        return mapOrdersToRows(orders);
    }

    @Transactional
    public void startOrder(Long id, Long staffId) {
        FoodOrder order = loadOrder(id);
        if (PREPARING.equals(order.getStatus())) {
            return;
        }
        if (!PAID.equals(order.getStatus())) {
            throw new BizException(5002, "只有已支付订单可以开始制作");
        }
        order.setStatus(PREPARING);
        order.setHandlerId(staffId);
        foodOrderMapper.updateById(order);
    }

    @Transactional
    public void completeOrder(Long id) {
        FoodOrder order = loadOrder(id);
        if (COMPLETED.equals(order.getStatus())) {
            return;
        }
        if (!PAID.equals(order.getStatus()) && !PREPARING.equals(order.getStatus())) {
            throw new BizException(5003, "只有已支付或制作中的订单可以完成");
        }
        order.setStatus(COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        foodOrderMapper.updateById(order);
    }

    private FoodOrder loadOrder(Long id) {
        FoodOrder order = foodOrderMapper.selectOne(new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getId, id)
            .eq(FoodOrder::getDeleted, 0)
            .last("LIMIT 1"));
        if (order == null) {
            throw new BizException(5001, "订单不存在");
        }
        return order;
    }

    public List<StaffOrderRow> handledOrders(Long staffId) {
        LambdaQueryWrapper<FoodOrder> wrapper = new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getDeleted, 0)
            .eq(FoodOrder::getHandlerId, staffId)
            .eq(FoodOrder::getStatus, COMPLETED)
            .orderByDesc(FoodOrder::getUpdatedAt);

        List<FoodOrder> orders = foodOrderMapper.selectList(wrapper);
        return mapOrdersToRows(orders);
    }

    public List<DiningTable> listTables(Long staffId, String status, Integer capacity) {
        List<Long> storeIds = resolveStoreIds(staffId);
        if (storeIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DiningTable> wrapper = new LambdaQueryWrapper<DiningTable>()
            .eq(DiningTable::getDeleted, 0)
            .in(DiningTable::getStoreId, storeIds);
        if (status != null && !status.isBlank()) {
            wrapper.eq(DiningTable::getStatus, status);
        }
        if (capacity != null) {
            wrapper.eq(DiningTable::getCapacity, capacity);
        }
        wrapper.orderByAsc(DiningTable::getTableNo);
        return diningTableMapper.selectList(wrapper);
    }

    public List<Cat> listCats(Long staffId, String status) {
        List<Long> storeIds = resolveStoreIds(staffId);
        if (storeIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Cat> wrapper = new LambdaQueryWrapper<Cat>()
            .eq(Cat::getDeleted, 0)
            .in(Cat::getStoreId, storeIds);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Cat::getStatus, status);
        }
        wrapper.orderByAsc(Cat::getName);
        return catMapper.selectList(wrapper);
    }

    @Transactional
    public void updateTableStatus(Long tableId, Long staffId, String newStatus, String reason) {
        DiningTable table = diningTableMapper.selectById(tableId);
        if (table == null || table.getDeleted() == 1) {
            throw new BizException(4004, "桌位不存在");
        }
        String oldStatus = table.getStatus();
        table.setStatus(newStatus);
        diningTableMapper.updateById(table);

        DiningTableStatusLog log = new DiningTableStatusLog();
        log.setTableId(tableId);
        log.setStoreId(table.getStoreId());
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setChangedBy(staffId);
        log.setReason(reason != null && !reason.isBlank() ? reason : "店员修改桌位状态");
        diningTableStatusLogMapper.insert(log);
    }

    public List<StaffReviewRow> listReviews(Long staffId) {
        List<Long> storeIds = resolveStoreIds(staffId);
        if (storeIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
            .eq(Review::getDeleted, 0)
            .in(Review::getStoreId, storeIds)
            .orderByDesc(Review::getCreatedAt);

        List<Review> reviews = reviewMapper.selectList(wrapper);
        if (reviews.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = reviews.stream().map(Review::getUserId).filter(Objects::nonNull).distinct().toList();
        final Map<Long, User> userMap;
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        } else {
            userMap = Collections.emptyMap();
        }

        List<Long> orderIds = reviews.stream().map(Review::getOrderId).filter(Objects::nonNull).distinct().toList();
        final Map<Long, FoodOrder> orderMap;
        if (!orderIds.isEmpty()) {
            List<FoodOrder> orders = foodOrderMapper.selectBatchIds(orderIds);
            orderMap = orders.stream().collect(Collectors.toMap(FoodOrder::getId, o -> o));
        } else {
            orderMap = Collections.emptyMap();
        }

        return reviews.stream().map(r -> {
            User user = userMap.get(r.getUserId());
            String customerName = user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "匿名顾客";
            FoodOrder order = orderMap.get(r.getOrderId());
            String orderNo = order != null ? order.getOrderNo() : "-";
            return new StaffReviewRow(
                r.getId(),
                customerName,
                orderNo,
                r.getRating(),
                r.getContent(),
                translateReviewStatus(r.getStatus()),
                r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""
            );
        }).toList();
    }

    private List<StaffOrderRow> mapOrdersToRows(List<FoodOrder> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tableIds = orders.stream().map(FoodOrder::getTableId).filter(Objects::nonNull).distinct().toList();
        final Map<Long, DiningTable> tableMap;
        if (!tableIds.isEmpty()) {
            List<DiningTable> tables = diningTableMapper.selectBatchIds(tableIds);
            tableMap = tables.stream().collect(Collectors.toMap(DiningTable::getId, t -> t));
        } else {
            tableMap = Collections.emptyMap();
        }

        return orders.stream().map(o -> {
            DiningTable table = tableMap.get(o.getTableId());
            String tableNo = table != null ? table.getTableNo() : "-";
            return new StaffOrderRow(
                o.getId(),
                o.getOrderNo(),
                buildOrderSummary(o.getId()),
                o.getTotalAmount(),
                tableNo,
                translateOrderStatus(o.getStatus()),
                o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
            );
        }).toList();
    }

    private String buildOrderSummary(Long orderId) {
        LambdaQueryWrapper<FoodOrderItem> wrapper = new LambdaQueryWrapper<FoodOrderItem>()
            .eq(FoodOrderItem::getOrderId, orderId);
        List<FoodOrderItem> items = foodOrderItemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            return "-";
        }
        return items.stream()
            .map(i -> i.getDishName() + " x" + i.getQuantity())
            .collect(Collectors.joining(" / "));
    }

    private String translateReservationStatus(String status) {
        return switch (status) {
            case "RESERVED" -> "已预约";
            case "CHECKED_IN" -> "已签到";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "NO_SHOW" -> "爽约";
            default -> status;
        };
    }

    private String translateOrderStatus(String status) {
        return switch (status) {
            case "CREATED" -> "待支付";
            case "PAID" -> "已支付/待制作";
            case "PREPARING" -> "制作中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "REFUNDING" -> "退款中";
            case "REFUNDED" -> "已退款";
            default -> status;
        };
    }

    public StaffReviewRow getOrderReview(Long orderId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
            .eq(Review::getDeleted, 0)
            .eq(Review::getOrderId, orderId)
            .last("LIMIT 1");
        Review review = reviewMapper.selectOne(wrapper);
        if (review == null) {
            return null;
        }

        User user = userMapper.selectById(review.getUserId());
        String customerName = user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "匿名顾客";
        FoodOrder order = foodOrderMapper.selectById(review.getOrderId());
        String orderNo = order != null ? order.getOrderNo() : "-";

        return new StaffReviewRow(
            review.getId(),
            customerName,
            orderNo,
            review.getRating(),
            review.getContent(),
            translateReviewStatus(review.getStatus()),
            review.getCreatedAt() != null ? review.getCreatedAt().toString() : ""
        );
    }

    private String translateReviewStatus(String status) {
        return switch (status) {
            case "VISIBLE" -> "显示中";
            case "HIDDEN" -> "已隐藏";
            case "PENDING" -> "待审核";
            default -> status;
        };
    }
}
