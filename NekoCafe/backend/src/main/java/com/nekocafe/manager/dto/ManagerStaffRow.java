package com.nekocafe.manager.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ManagerStaffRow(
    Long userStoreRoleId,
    Long userId,
    String username,
    String nickname,
    String phone,
    String email,
    String roleCode,
    String status,
    LocalDate todayShiftDate,
    LocalTime todayShiftStartTime,
    LocalTime todayShiftEndTime,
    String todayShiftStatus,
    String activeLeaveStatus
) {
}
