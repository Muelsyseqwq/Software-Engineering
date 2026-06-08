package com.nekocafe.staff.dto;

import java.math.BigDecimal;

public record StaffOrderRow(
    Long id,
    String orderNo,
    String summary,
    BigDecimal amount,
    String tableNo,
    String status,
    String createdAt
) {
}
