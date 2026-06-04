package com.nekocafe.cat.dto;

public record CatResponse(
    Long id,
    String name,
    String breed,
    Integer age,
    String gender,
    String personality,
    String healthStatus,
    String photoUrl,
    String description,
    String status
) {
}
