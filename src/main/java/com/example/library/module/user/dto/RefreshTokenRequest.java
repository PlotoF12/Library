package com.example.library.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 刷新Token请求DTO */
@Data
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}
