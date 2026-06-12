package com.nekocafe.recommend.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.recommend.dto.RecommendationFeedResponse;
import com.nekocafe.recommend.service.RecommendService;
import com.nekocafe.security.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "recommend", "status", "ready"));
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<RecommendationFeedResponse> customerRecommendations(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) BigDecimal lat,
        @RequestParam(required = false) BigDecimal lng,
        @RequestParam(required = false) Integer limit
    ) {
        return ApiResult.ok(recommendService.customerRecommendations(principal.userId(), lat, lng, limit));
    }
}
