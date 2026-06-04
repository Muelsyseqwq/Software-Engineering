package com.nekocafe.manager.dto;

public record ManagerStoreInfo(
    Long id,
    String name,
    String city,
    String address,
    String phone,
    String status
) {
}
