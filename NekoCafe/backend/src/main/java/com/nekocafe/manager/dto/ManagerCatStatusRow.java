package com.nekocafe.manager.dto;

public record ManagerCatStatusRow(
    Long id,
    String name,
    String breed,
    Integer age,
    String gender,
    String healthStatus,
    String status,
    String photoUrl,
    String description
) {
}
