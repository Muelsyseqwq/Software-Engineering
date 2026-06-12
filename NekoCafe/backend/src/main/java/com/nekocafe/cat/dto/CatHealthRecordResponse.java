package com.nekocafe.cat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CatHealthRecordResponse(
    Long id,
    Long catId,
    LocalDate recordDate,
    BigDecimal weight,
    String vaccinium,
    String interact,
    String note,
    Long recordedBy,
    LocalDateTime createdAt
) {
}
