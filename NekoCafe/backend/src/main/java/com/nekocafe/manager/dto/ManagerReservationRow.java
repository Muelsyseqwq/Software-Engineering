package com.nekocafe.manager.dto;

import java.time.LocalDateTime;

public record ManagerReservationRow(
    Long id,
    String reservationNo,
    String customerName,
    String contactPhone,
    Integer partySize,
    String tableNo,
    String slotTime,
    String status,
    String remark,
    LocalDateTime createdAt
) {
}
