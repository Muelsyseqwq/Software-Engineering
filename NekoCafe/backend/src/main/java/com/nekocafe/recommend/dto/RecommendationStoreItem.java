package com.nekocafe.recommend.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationStoreItem(
    Integer rank,
    Long storeId,
    String storeName,
    String city,
    String businessArea,
    String address,
    String status,
    BigDecimal distanceKm,
    Integer score,
    List<String> tags,
    List<String> reasons,
    List<RecommendationHighlight> dishHighlights,
    List<RecommendationHighlight> catHighlights,
    List<RecommendationHighlight> activityHighlights,
    String primaryActionText
) {
}
