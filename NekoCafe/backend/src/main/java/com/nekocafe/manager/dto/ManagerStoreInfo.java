package com.nekocafe.manager.dto;

import java.time.LocalTime;

public record ManagerStoreInfo(
    Long id,
    String name,
    String city,
    String address,
    String phone,
    LocalTime openingTime,
    LocalTime closingTime,
    String status,
    String description
) {
}
