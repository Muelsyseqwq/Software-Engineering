package com.nekocafe.recommend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationFeedResponse(
    LocalDateTime generatedAt,
    String summary,
    List<RecommendationStoreItem> items
) {
}
