package com.nekocafe.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "账号不能为空")
    String account,

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度需为 6-72 位")
    String password
) {
}
