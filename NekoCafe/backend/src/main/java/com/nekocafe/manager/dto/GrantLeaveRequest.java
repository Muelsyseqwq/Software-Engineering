package com.nekocafe.manager.dto;

import java.time.LocalDate;

public record GrantLeaveRequest(
    String leaveType,
    LocalDate startDate,
    LocalDate endDate,
    String reason
) {
}
