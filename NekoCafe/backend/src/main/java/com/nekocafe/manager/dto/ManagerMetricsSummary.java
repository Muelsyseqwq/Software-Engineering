package com.nekocafe.manager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ManagerMetricsSummary(
    Long storeId,
    LocalDate from,
    LocalDate to,
    BigDecimal revenue,
    Long paidOrderCount,
    Long completedOrderCount,
    Long reservationCount,
    Long checkedInReservationCount,
    Long tableCount,
    BigDecimal tableTurnoverRate,
    BigDecimal areaSquareMeter,
    BigDecimal revenuePerSquareMeter,
    BigDecimal averageOrderValue
) {
}
