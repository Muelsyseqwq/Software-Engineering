package com.nekocafe.manager.dto;

public record ManagerTableRow(
    Long id,
    String tableNo,
    Integer capacity,
    String area,
    String status
) {
}
