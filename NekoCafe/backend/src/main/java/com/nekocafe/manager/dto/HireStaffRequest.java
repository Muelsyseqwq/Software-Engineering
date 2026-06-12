package com.nekocafe.manager.dto;

public record HireStaffRequest(
    String username,
    String password,
    String nickname,
    String phone,
    String email,
    String roleCode
) {
}
