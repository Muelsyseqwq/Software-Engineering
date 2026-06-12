package com.nekocafe.recommend.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.recommend.ai.AiReasonProperties;
import com.nekocafe.recommend.ai.RecommendationReasonGenerator;
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
    private final AiReasonProperties aiReasonProperties;
    private final RecommendationReasonGenerator reasonGenerator;

    public RecommendController(
        RecommendService recommendService,
        AiReasonProperties aiReasonProperties,
        RecommendationReasonGenerator reasonGenerator
    ) {
        this.recommendService = recommendService;
        this.aiReasonProperties = aiReasonProperties;
        this.reasonGenerator = reasonGenerator;
    }

    @GetMapping("/status")
    public ApiResult<Map<String, Object>> status() {
        return ApiResult.ok(Map.ofEntries(
            Map.entry("module", "recommend"),
            Map.entry("status", "ready"),
            Map.entry("aiEnabled", aiReasonProperties.isEnabled()),
            Map.entry("aiConfigured", aiReasonProperties.getApiKey() != null && !aiReasonProperties.getApiKey().isBlank()),
            Map.entry("aiAvailable", aiReasonProperties.available()),
            Map.entry("aiProvider", aiReasonProperties.getProvider()),
            Map.entry("aiBaseUrl", aiReasonProperties.getBaseUrl()),
            Map.entry("aiModel", aiReasonProperties.getModel()),
            Map.entry("aiTimeoutSeconds", aiReasonProperties.getTimeoutSeconds()),
            Map.entry("aiMaxTokens", aiReasonProperties.getMaxTokens()),
            Map.entry("aiLastStatus", reasonGenerator.lastStatus()),
            Map.entry("aiLastErrorType", reasonGenerator.lastErrorType()),
            Map.entry("aiLastErrorMessage", reasonGenerator.lastErrorMessage())
        ));
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
