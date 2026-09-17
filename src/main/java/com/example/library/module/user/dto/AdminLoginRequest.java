package com.example.library.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 管理员登录请求DTO */
@Data
public class AdminLoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
