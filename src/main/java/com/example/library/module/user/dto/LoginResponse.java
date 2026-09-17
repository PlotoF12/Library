package com.example.library.module.user.dto;

import lombok.Builder;
import lombok.Data;

/** 登录响应DTO */
@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserInfo userInfo;

    @Data
    @Builder
    public static class UserInfo {
        private Long userId;
        private String studentNo;
        private String name;
        private Long adminId;
        private String username;
    }
}
