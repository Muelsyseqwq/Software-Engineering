package com.nekocafe.manager.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ManagerShiftRequest(
    Long userId,
    String roleCode,
    LocalDate shiftDate,
    LocalTime startTime,
    LocalTime endTime,
    String status,
    String remark
) {
}
