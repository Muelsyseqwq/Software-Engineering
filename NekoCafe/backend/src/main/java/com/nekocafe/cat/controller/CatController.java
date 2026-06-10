package com.nekocafe.cat.controller;

import com.nekocafe.cat.dto.CatPhotoUploadResponse;
import com.nekocafe.cat.dto.CatRequest;
import com.nekocafe.cat.dto.CatResponse;
import com.nekocafe.cat.service.CatPhotoStorageService;
import com.nekocafe.cat.service.CatService;
import com.nekocafe.common.result.ApiResult;
import com.nekocafe.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cats")
@PreAuthorize("hasRole('CAT_CARETAKER')")
public class CatController {

    private final CatService catService;
    private final CatPhotoStorageService catPhotoStorageService;

    public CatController(CatService catService, CatPhotoStorageService catPhotoStorageService) {
        this.catService = catService;
        this.catPhotoStorageService = catPhotoStorageService;
    }

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<CatPhotoUploadResponse> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return ApiResult.ok(catPhotoStorageService.store(file));
    }

    @GetMapping
    public ApiResult<List<CatResponse>> listCats(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(catService.listCats(principal.userId()));
    }

    @GetMapping("/{id}")
    public ApiResult<CatResponse> getCat(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        return ApiResult.ok(catService.getCat(principal.userId(), id));
    }

    @PostMapping
    public ApiResult<CatResponse> createCat(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody CatRequest payload) {
        return ApiResult.ok(catService.createCat(principal.userId(), payload));
    }

    @PutMapping("/{id}")
    public ApiResult<CatResponse> updateCat(@AuthenticationPrincipal AuthPrincipal principal,
                                             @PathVariable Long id,
                                             @Valid @RequestBody CatRequest payload) {
        return ApiResult.ok(catService.updateCat(principal.userId(), id, payload));
    }

    @PatchMapping("/{id}/health-status")
    public ApiResult<CatResponse> updateHealthStatus(@AuthenticationPrincipal AuthPrincipal principal,
                                                     @PathVariable Long id,
                                                     @RequestBody Map<String, String> payload) {
        return ApiResult.ok(catService.updateHealthStatus(principal.userId(), id, payload.get("healthStatus")));
    }

    @PatchMapping("/{id}/status")
    public ApiResult<CatResponse> updateStatus(@AuthenticationPrincipal AuthPrincipal principal,
                                               @PathVariable Long id,
                                               @RequestBody Map<String, String> payload) {
        return ApiResult.ok(catService.updateStatus(principal.userId(), id, payload.get("status")));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteCat(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        catService.deleteCat(principal.userId(), id);
        return ApiResult.ok();
    }
}
