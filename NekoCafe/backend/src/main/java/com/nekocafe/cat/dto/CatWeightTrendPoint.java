package com.nekocafe.cat.dto;

import java.math.BigDecimal;

public record CatWeightTrendPoint(
    String label,
    BigDecimal value
) {
}
