package com.nekocafe.store.controller;

import com.nekocafe.common.result.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/store")
public class StoreController {

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "store", "status", "ready"));
    }
}
