package com.example.library.module.user.controller;

import com.example.library.common.annotation.RateLimit;
import com.example.library.common.response.ApiResponse;
import com.example.library.module.user.dto.*;
import com.example.library.module.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 — 注册、登录、Token刷新、登出。
 *
 * <p>所有接口均无需认证（白名单），但登录/注册接口有限流保护。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 学生注册 — 限流：每分钟最多3次 */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimit(key = "register", limit = 3, window = 60)
    public ApiResponse<StudentProfileVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    /** 学生登录 — 限流：每分钟最多5次（防暴力破解） */
    @PostMapping("/login")
    @RateLimit(key = "login", limit = 5, window = 60)
    public ApiResponse<LoginResponse> studentLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.studentLogin(request));
    }

    /** 管理员登录 — 限流：每分钟最多5次 */
    @PostMapping("/admin/login")
    @RateLimit(key = "admin_login", limit = 5, window = 60)
    public ApiResponse<LoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success("登录成功", authService.adminLogin(request));
    }

    /** 刷新Token */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Token刷新成功", authService.refreshToken(request));
    }

    /** 登出 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false)
                                             String authHeader) {
        authService.logout(authHeader);
        return ApiResponse.success("已登出", null);
    }
}
