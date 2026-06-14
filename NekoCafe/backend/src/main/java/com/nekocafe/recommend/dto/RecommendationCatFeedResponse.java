package com.nekocafe.recommend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationCatFeedResponse(
    LocalDateTime generatedAt,
    String summary,
    List<RecommendationCatItem> items
) {
}
