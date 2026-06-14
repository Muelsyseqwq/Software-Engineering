package com.nekocafe.cat.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CatHealthRecordRequest(
    LocalDate recordDate,

    @DecimalMin(value = "0.20", message = "体重不能小于 0.20kg")
    @DecimalMax(value = "20.00", message = "体重不能超过 20.00kg")
    @Digits(integer = 2, fraction = 2, message = "体重最多保留两位小数")
    BigDecimal weight,

    @Size(max = 255, message = "疫苗信息不能超过 255 位")
    String vaccinium,

    @Size(max = 255, message = "互动记录不能超过 255 位")
    String interact,

    @Size(max = 500, message = "备注不能超过 500 位")
    String note
) {
}
