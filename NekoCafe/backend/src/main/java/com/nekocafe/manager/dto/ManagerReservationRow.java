package com.nekocafe.manager.dto;

public record ManagerReservationRow(
    Long id,
    String reservationNo,
    String customerName,
    Integer partySize,
    String slotTime,
    String status
) {
}
