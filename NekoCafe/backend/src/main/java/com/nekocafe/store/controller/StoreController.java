package com.nekocafe.store.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.store.service.StoreService;
import com.nekocafe.store.service.StoreService.StoreDetailResponse;
import com.nekocafe.store.service.StoreService.StoreSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/store")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "store", "status", "ready"));
    }

    @GetMapping
    public ApiResult<List<StoreSummaryResponse>> list() {
        return ApiResult.ok(storeService.listStores());
    }

    @GetMapping("/{id}")
    public ApiResult<StoreDetailResponse> detail(@PathVariable Long id) {
        return ApiResult.ok(storeService.detail(id));
    }
}
