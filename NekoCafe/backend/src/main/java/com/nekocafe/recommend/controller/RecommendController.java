package com.nekocafe.recommend.controller;

import com.nekocafe.common.result.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "recommend", "status", "ready"));
    }
}
