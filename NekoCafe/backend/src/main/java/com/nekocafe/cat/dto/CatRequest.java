package com.nekocafe.cat.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CatRequest(
    @NotBlank(message = "猫咪名字不能为空")
    @Size(max = 64, message = "猫咪名字不能超过 64 位")
    String name,

    @Size(max = 64, message = "品种不能超过 64 位")
    String breed,

    @Min(value = 0, message = "年龄不能小于 0")
    @Max(value = 40, message = "年龄不能超过 40")
    Integer age,

    @DecimalMin(value = "0.20", message = "体重不能小于 0.20kg")
    @DecimalMax(value = "20.00", message = "体重不能超过 20.00kg")
    @Digits(integer = 2, fraction = 2, message = "体重最多保留两位小数")
    BigDecimal weight,

    @Size(max = 16, message = "性别不能超过 16 位")
    String gender,

    @Size(max = 255, message = "性格描述不能超过 255 位")
    String personality,

    @Size(max = 255, message = "互动记录不能超过 255 位")
    String interact,

    @Size(max = 32, message = "健康状态不能超过 32 位")
    String healthStatus,

    @Size(max = 255, message = "疫苗信息不能超过 255 位")
    String vaccinium,

    @Size(max = 255, message = "照片地址不能超过 255 位")
    String photoUrl,

    String description,

    @Size(max = 32, message = "档案状态不能超过 32 位")
    String status
) {
}
