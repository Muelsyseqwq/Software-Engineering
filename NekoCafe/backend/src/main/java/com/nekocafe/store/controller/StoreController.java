package com.nekocafe.store.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.store.service.StorePhotoStorageService;
import com.nekocafe.store.service.StorePhotoStorageService.UploadResult;
import com.nekocafe.store.service.StoreService;
import com.nekocafe.store.service.StoreService.CreateStoreRequest;
import com.nekocafe.store.service.StoreService.StoreDetailResponse;
import com.nekocafe.store.service.StoreService.StoreSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
public class StoreController {

    private final StoreService storeService;
    private final StorePhotoStorageService storePhotoStorageService;

    public StoreController(StoreService storeService,
                           StorePhotoStorageService storePhotoStorageService) {
        this.storeService = storeService;
        this.storePhotoStorageService = storePhotoStorageService;
    }

    // ---- public endpoints ----

    @GetMapping("/api/store/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "store", "status", "ready"));
    }

    @GetMapping("/api/store")
    public ApiResult<List<StoreSummaryResponse>> list() {
        return ApiResult.ok(storeService.listStores());
    }

    @GetMapping("/api/store/nearby")
    public ApiResult<List<StoreService.NearbyStoreResponse>> nearby(
        @RequestParam BigDecimal lat,
        @RequestParam BigDecimal lng
    ) {
        return ApiResult.ok(storeService.nearbyStores(lat, lng));
    }

    @GetMapping("/api/store/{id}")
    public ApiResult<StoreDetailResponse> detail(@PathVariable Long id) {
        return ApiResult.ok(storeService.detail(id));
    }

    // ---- admin endpoints ----

    @GetMapping("/api/admin/stores")
    @PreAuthorize("hasRole('HQ_OPERATOR')")
    public ApiResult<List<StoreSummaryResponse>> adminList() {
        return ApiResult.ok(storeService.adminList());
    }

    @PostMapping("/api/admin/stores")
    @PreAuthorize("hasRole('HQ_OPERATOR')")
    public ApiResult<StoreDetailResponse> create(@RequestBody CreateStoreRequest request) {
        return ApiResult.ok(storeService.create(request));
    }

    @PutMapping("/api/admin/stores/{id}")
    @PreAuthorize("hasRole('HQ_OPERATOR')")
    public ApiResult<StoreDetailResponse> update(@PathVariable Long id,
                                                  @RequestBody CreateStoreRequest request) {
        return ApiResult.ok(storeService.update(id, request));
    }

    @DeleteMapping("/api/admin/stores/{id}")
    @PreAuthorize("hasRole('HQ_OPERATOR')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return ApiResult.ok(null);
    }

    @PostMapping("/api/admin/stores/upload")
    @PreAuthorize("hasRole('HQ_OPERATOR')")
    public ApiResult<UploadResult> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return ApiResult.ok(storePhotoStorageService.store(file));
    }
}
