package com.nekocafe.recommend.dto;

import java.util.List;

public record RecommendationCatItem(
    int rank,
    Long catId,
    String catName,
    String breed,
    String photoUrl,
    Long storeId,
    String storeName,
    String personality,
    String interact,
    String healthStatus,
    String status,
    int score,
    List<String> tags,
    List<String> reasons,
    String primaryActionText
) {
}
