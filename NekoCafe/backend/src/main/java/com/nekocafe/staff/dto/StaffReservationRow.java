package com.nekocafe.staff.dto;

public record StaffReservationRow(
    Long id,
    String reservationNo,
    String customerName,
    String customerPhone,
    Integer partySize,
    String timeSlot,
    String remark,
    String status
) {
}
