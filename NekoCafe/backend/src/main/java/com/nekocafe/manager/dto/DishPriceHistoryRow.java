package com.nekocafe.manager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DishPriceHistoryRow(
    Long id,
    Long dishId,
    BigDecimal oldPrice,
    BigDecimal newPrice,
    Long changedBy,
    String reason,
    LocalDateTime createdAt
) {
}
