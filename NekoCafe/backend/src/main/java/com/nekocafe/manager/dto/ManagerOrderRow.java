package com.nekocafe.manager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ManagerOrderRow(
    Long id,
    String orderNo,
    String customerName,
    String tableNo,
    String reservationNo,
    BigDecimal totalAmount,
    String status,
    String refundStatus,
    String refundReason,
    LocalDateTime paidAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    String itemSummary
) {
}
