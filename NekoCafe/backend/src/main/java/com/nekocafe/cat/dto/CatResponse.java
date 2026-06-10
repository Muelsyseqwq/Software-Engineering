package com.nekocafe.cat.dto;

import java.math.BigDecimal;

public record CatResponse(
    Long id,
    String name,
    String breed,
    Integer age,
    BigDecimal weight,
    String gender,
    String personality,
    String interact,
    String healthStatus,
    String vaccinium,
    String photoUrl,
    String description,
    String status
) {
}
