package com.nekocafe.manager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ManagerDishSalesSummary(
    Long storeId,
    LocalDate from,
    LocalDate to,
    Long totalQuantity,
    BigDecimal totalRevenue,
    List<ManagerDishSalesRow> items
) {
}
