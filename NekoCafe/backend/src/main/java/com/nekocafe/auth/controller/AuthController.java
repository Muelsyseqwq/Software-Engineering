package com.nekocafe.auth.controller;

import com.nekocafe.auth.dto.AuthResponse;
import com.nekocafe.auth.dto.AuthUserResponse;
import com.nekocafe.auth.dto.LoginRequest;
import com.nekocafe.auth.dto.RegisterRequest;
import com.nekocafe.auth.service.AuthService;
import com.nekocafe.common.result.ApiResult;
import com.nekocafe.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "auth", "status", "ready"));
    }

    @PostMapping("/register")
    public ApiResult<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResult<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResult<AuthUserResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(authService.me(principal));
    }
}
