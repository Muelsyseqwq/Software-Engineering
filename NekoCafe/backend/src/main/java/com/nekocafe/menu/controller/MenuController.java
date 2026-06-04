package com.nekocafe.menu.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.menu.service.MenuService;
import com.nekocafe.menu.service.MenuService.MenuCategoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "menu", "status", "ready"));
    }

    @GetMapping("/stores/{storeId}")
    public ApiResult<List<MenuCategoryResponse>> storeMenu(@PathVariable Long storeId) {
        return ApiResult.ok(menuService.storeMenu(storeId));
    }
}
