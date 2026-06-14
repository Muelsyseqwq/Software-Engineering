package com.nekocafe.manager.dto;

import java.math.BigDecimal;

public record UpdateDishPriceRequest(
    BigDecimal newPrice,
    String reason
) {
}
