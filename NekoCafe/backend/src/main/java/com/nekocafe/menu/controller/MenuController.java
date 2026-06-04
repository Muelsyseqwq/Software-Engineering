package com.nekocafe.menu.controller;

import com.nekocafe.common.result.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "menu", "status", "ready"));
    }
}
