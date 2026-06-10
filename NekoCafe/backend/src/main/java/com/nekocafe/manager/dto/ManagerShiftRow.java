package com.nekocafe.manager.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ManagerShiftRow(
    Long id,
    Long userId,
    String username,
    String nickname,
    String roleCode,
    LocalDate shiftDate,
    LocalTime startTime,
    LocalTime endTime,
    String status,
    String remark
) {
}
