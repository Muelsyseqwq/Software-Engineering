package com.nekocafe.manager.dto;

import java.math.BigDecimal;

public record ManagerOrderItemRow(
    Long id,
    Long dishId,
    String dishName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal subtotal
) {
}
