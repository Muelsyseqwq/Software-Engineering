package com.nekocafe.auth.dto;

import java.util.List;

public record AuthUserResponse(
    Long id,
    String username,
    String nickname,
    String phone,
    String email,
    List<String> roles,
    Long storeId,
    String storeName,
    List<String> storeNames,
    List<AuthAssignedStore> stores
) {
    public record AuthAssignedStore(Long id, String name, String city, String address) {}
}
