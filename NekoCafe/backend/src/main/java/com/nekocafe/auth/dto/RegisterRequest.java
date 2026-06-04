package com.nekocafe.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "用户名需为 3-32 位字母、数字或下划线")
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度需为 6-72 位")
    String password,

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称不能超过 64 位")
    String nickname,

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    String phone,

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过 128 位")
    String email
) {
}
