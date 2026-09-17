package com.example.library.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 注册请求DTO */
@Data
public class RegisterRequest {
    @NotBlank @Size(min = 6, max = 20)
    private String studentNo;

    @NotBlank @Size(min = 2, max = 50)
    private String name;

    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank @Size(min = 8, max = 32)
    private String password;
}
