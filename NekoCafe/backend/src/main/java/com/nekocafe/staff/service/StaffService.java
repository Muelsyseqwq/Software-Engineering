package com.nekocafe.staff.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.entity.FoodOrderItem;
import com.nekocafe.order.mapper.FoodOrderItemMapper;
import com.nekocafe.order.mapper.FoodOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffService {

    private static final String PAID = "PAID";
    private static final String PREPARING = "PREPARING";
    private static final String COMPLETED = "COMPLETED";

    private final FoodOrderMapper orderMapper;
    private final FoodOrderItemMapper orderItemMapper;

    public StaffService(FoodOrderMapper orderMapper, FoodOrderItemMapper orderItemMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    public List<StaffReservationRow> todayReservations() {
        return List.of(
            new StaffReservationRow(1L, "R20260604001", "猫爪布丁", "13800000000", 2, LocalDateTime.now().plusHours(1).toString(), "待签到")
        );
    }

    public void checkInReservation(Long id) {
        // 框架占位：后续由店员模块负责人接入预约签到状态流转。
    }

    public List<StaffOrderRow> pendingOrders() {
        return orderMapper.selectList(new LambdaQueryWrapper<FoodOrder>()
                .eq(FoodOrder::getDeleted, 0)
                .in(FoodOrder::getStatus, List.of(PAID, PREPARING))
                .orderByAsc(FoodOrder::getCreatedAt)
                .orderByAsc(FoodOrder::getId))
            .stream()
            .map(this::toStaffOrderRow)
            .toList();
    }

    @Transactional
    public void startOrder(Long id) {
        FoodOrder order = loadOrder(id);
        if (PREPARING.equals(order.getStatus())) {
            return;
        }
        if (!PAID.equals(order.getStatus())) {
            throw new BizException(5002, "只有已支付订单可以开始制作");
        }
        order.setStatus(PREPARING);
        orderMapper.updateById(order);
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
        orderMapper.updateById(order);
    }

    private FoodOrder loadOrder(Long id) {
        FoodOrder order = orderMapper.selectOne(new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getId, id)
            .eq(FoodOrder::getDeleted, 0)
            .last("LIMIT 1"));
        if (order == null) {
            throw new BizException(5001, "订单不存在");
        }
        return order;
    }

    private StaffOrderRow toStaffOrderRow(FoodOrder order) {
        return new StaffOrderRow(
            order.getId(),
            order.getOrderNo(),
            formatOrderSummary(order.getId()),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreatedAt() == null ? "" : order.getCreatedAt().toString()
        );
    }

    private String formatOrderSummary(Long orderId) {
        List<FoodOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<FoodOrderItem>()
            .eq(FoodOrderItem::getOrderId, orderId)
            .orderByAsc(FoodOrderItem::getId));
        if (items.isEmpty()) {
            return "暂无菜品";
        }
        return items.stream()
            .map(item -> item.getDishName() + " x" + item.getQuantity())
            .collect(Collectors.joining(" / "));
    }

    public record StaffReservationRow(Long id, String reservationNo, String customerName, String customerPhone, int partySize, String reservedTime, String status) {
    }

    public record StaffOrderRow(Long id, String orderNo, String summary, BigDecimal amount, String status, String createdAt) {
    }
}
