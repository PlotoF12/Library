package com.example.library.module.user.service;

import com.example.library.module.user.dto.*;

/**
 * 认证服务接口
 */
public interface AuthService {

    /** 学生注册 */
    StudentProfileVO register(RegisterRequest request);

    /** 学生登录 */
    LoginResponse studentLogin(LoginRequest request);

    /** 管理员登录 */
    LoginResponse adminLogin(AdminLoginRequest request);

    /** 刷新Token */
    LoginResponse refreshToken(RefreshTokenRequest request);

    /** 登出 */
    void logout(String accessToken);
}
