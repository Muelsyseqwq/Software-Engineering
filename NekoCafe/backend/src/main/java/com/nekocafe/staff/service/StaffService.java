package com.nekocafe.staff.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StaffService {

    public List<StaffReservationRow> todayReservations() {
        return List.of(
            new StaffReservationRow(1L, "R20260604001", "猫爪布丁", "13800000000", 2, LocalDateTime.now().plusHours(1).toString(), "待签到")
        );
    }

    public void checkInReservation(Long id) {
        // 框架占位：后续由店员模块负责人接入预约签到状态流转。
    }

    public List<StaffOrderRow> pendingOrders() {
        return List.of(
            new StaffOrderRow(1L, "O20260604001", "拿铁 x1 / 猫爪蛋糕 x1", new BigDecimal("58.00"), "待制作", LocalDateTime.now().minusMinutes(20).toString())
        );
    }

    public void startOrder(Long id) {
        // 框架占位：后续由店员模块负责人接入订单开始制作状态流转。
    }

    public void completeOrder(Long id) {
        // 框架占位：后续由店员模块负责人接入订单完成状态流转。
    }

    public record StaffReservationRow(Long id, String reservationNo, String customerName, String customerPhone, int partySize, String reservedTime, String status) {
    }

    public record StaffOrderRow(Long id, String orderNo, String summary, BigDecimal amount, String status, String createdAt) {
    }
}
