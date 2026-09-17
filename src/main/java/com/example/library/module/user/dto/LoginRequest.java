package com.example.library.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求DTO */
@Data
public class LoginRequest {
    @NotBlank(message = "学号不能为空")
    private String studentNo;

    @NotBlank(message = "密码不能为空")
    private String password;
}
