package com.nekocafe.manager.dto;

import java.time.LocalDateTime;

public record ManagerActivityRow(
    Long activityStoreId,
    Long activityId,
    String title,
    String type,
    String description,
    String coverUrl,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String activityStatus,
    String acceptStatus,
    LocalDateTime handledAt,
    String handleRemark
) {
}
