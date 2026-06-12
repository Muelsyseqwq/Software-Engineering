package com.nekocafe.manager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ManagerOrderDetail(
    Long id,
    String orderNo,
    String customerName,
    String tableNo,
    String reservationNo,
    BigDecimal totalAmount,
    String status,
    String refundStatus,
    String remark,
    LocalDateTime paidAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    Integer reviewRating,
    String reviewContent,
    LocalDateTime reviewCreatedAt,
    List<ManagerOrderItemRow> items
) {
}
