package com.nekocafe.manager.dto;

import java.math.BigDecimal;

public record ManagerDishSalesRow(
    Long dishId,
    String dishName,
    Long quantity,
    Long orderCount,
    BigDecimal revenue
) {
}
