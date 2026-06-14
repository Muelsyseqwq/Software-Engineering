package com.nekocafe.manager.dto;

import java.math.BigDecimal;

public record ManagerDishRow(
    Long id,
    Long categoryId,
    String name,
    BigDecimal price,
    Integer stock,
    String status,
    String description,
    String imageUrl
) {
}
