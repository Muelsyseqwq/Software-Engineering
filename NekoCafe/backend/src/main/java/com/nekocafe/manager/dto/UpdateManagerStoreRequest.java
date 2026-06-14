package com.nekocafe.manager.dto;

import java.time.LocalTime;

public record UpdateManagerStoreRequest(
    String name,
    String city,
    String address,
    String phone,
    LocalTime openingTime,
    LocalTime closingTime,
    String description
) {
}
