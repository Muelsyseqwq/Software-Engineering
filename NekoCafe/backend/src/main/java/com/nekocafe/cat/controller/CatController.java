package com.nekocafe.cat.controller;

import com.nekocafe.cat.service.CatService;
import com.nekocafe.cat.service.CatService.CatProfile;
import com.nekocafe.common.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cats")
@PreAuthorize("hasRole('CAT_CARETAKER')")
public class CatController {

    private final CatService catService;

    public CatController(CatService catService) {
        this.catService = catService;
    }

    @GetMapping
    public ApiResult<List<CatProfile>> listCats() {
        return ApiResult.ok(catService.listCats());
    }

    @GetMapping("/{id}")
    public ApiResult<CatProfile> getCat(@PathVariable Long id) {
        return ApiResult.ok(catService.getCat(id));
    }

    @PostMapping
    public ApiResult<CatProfile> createCat(@RequestBody CatProfile payload) {
        return ApiResult.ok(catService.createCat(payload));
    }

    @PutMapping("/{id}")
    public ApiResult<CatProfile> updateCat(@PathVariable Long id, @RequestBody CatProfile payload) {
        return ApiResult.ok(catService.updateCat(id, payload));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteCat(@PathVariable Long id) {
        catService.deleteCat(id);
        return ApiResult.ok();
    }
}
