package com.example.library.module.user.service.impl;

import com.example.library.common.exception.BizException;
import com.example.library.module.user.dto.*;
import com.example.library.module.user.entity.Admin;
import com.example.library.module.user.entity.Student;
import com.example.library.module.user.repository.AdminRepository;
import com.example.library.module.user.repository.StudentRepository;
import com.example.library.module.user.service.AuthService;
import com.example.library.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现 — 处理注册、登录、Token刷新、登出。
 *
 * <p>业务规则：
 * <ul>
 *   <li>注册：学号和手机号必须唯一，密码 BCrypt 加密存储</li>
 *   <li>登录：验证密码后签发 Access Token (2h) + Refresh Token (7天)</li>
 *   <li>Token刷新：Refresh Token Rotation，旧Token立即失效</li>
 *   <li>登出：JWT jti 加入 Redis 黑名单</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    // ---- 错误码 ----
    private static final int ERR_STUDENT_NO_EXISTS = 11001;
    private static final int ERR_PHONE_EXISTS = 11002;
    private static final int ERR_CREDENTIALS = 11003;
    private static final int ERR_ACCOUNT_DISABLED = 11004;
    private static final int ERR_REFRESH_TOKEN_INVALID = 11005;

    /**
     * 学生注册。
     * 校验学号和手机号唯一性 → BCrypt 加密密码 → 保存。
     */
    @Override
    @Transactional
    public StudentProfileVO register(RegisterRequest request) {
        // 1. 校验学号唯一
        if (studentRepository.existsByStudentNo(request.getStudentNo())) {
            throw new BizException(ERR_STUDENT_NO_EXISTS, "学号已被注册");
        }
        // 2. 校验手机号唯一
        if (studentRepository.existsByPhone(request.getPhone())) {
            throw new BizException(ERR_PHONE_EXISTS, "手机号已被注册");
        }

        // 3. 保存学生
        Student student = Student.builder()
                .studentNo(request.getStudentNo())
                .name(request.getName())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .violationCount(0)
                .build();
        student = studentRepository.save(student);

        log.info("学生注册成功: studentNo={}, id={}", student.getStudentNo(), student.getId());
        return buildProfileVO(student);
    }

    /**
     * 学生登录。
     * 查询学生 → 验证密码 → 签发 Token → 返回。
     */
    @Override
    public LoginResponse studentLogin(LoginRequest request) {
        Student student = studentRepository.findByStudentNo(request.getStudentNo())
                .orElseThrow(() -> new BizException(ERR_CREDENTIALS, "学号或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), student.getPasswordHash())) {
            throw new BizException(ERR_CREDENTIALS, "学号或密码错误");
        }

        return buildLoginResponse(
                "student_" + student.getId(),
                "STUDENT",
                buildStudentUserInfo(student)
        );
    }

    /**
     * 管理员登录。
     */
    @Override
    public LoginResponse adminLogin(AdminLoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BizException(ERR_CREDENTIALS, "用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BizException(ERR_CREDENTIALS, "用户名或密码错误");
        }

        log.info("管理员登录: username={}", admin.getUsername());
        return buildLoginResponse(
                "admin_" + admin.getId(),
                "ADMIN",
                LoginResponse.UserInfo.builder()
                        .adminId(admin.getId())
                        .username(admin.getUsername())
                        .name(admin.getName())
                        .build()
        );
    }

    /**
     * 刷新 Token — Refresh Token Rotation。
     *
     * <p>安全设计：每次刷新时旧的 Refresh Token 立即失效，
     * 签发新的 Access Token 和 Refresh Token。
     * 这样即使 Refresh Token 被窃取，一旦用户正常刷新，
     * 窃取者的 Token 就失效了。
     */
    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String oldRefreshToken = request.getRefreshToken();
        String key = REFRESH_TOKEN_PREFIX + oldRefreshToken;
        String subject = redisTemplate.opsForValue().get(key);

        if (subject == null) {
            throw new BizException(ERR_REFRESH_TOKEN_INVALID, "Refresh Token无效或已过期");
        }

        // 旧 Refresh Token 立即失效（Rotation）
        redisTemplate.delete(key);

        // 解析 subject 获取角色信息用于签发新 Token
        String role = subject.startsWith("admin_") ? "ADMIN" : "STUDENT";

        // 签发新 Token 对
        String newAccessToken = jwtTokenProvider.generateAccessToken(subject, role);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();

        // 存储新 Refresh Token
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + newRefreshToken,
                subject,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.SECONDS
        );

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    /**
     * 登出 — 将 JWT 的 jti 加入 Redis 黑名单。
     * 黑名单 TTL = Token 剩余有效时间，避免永久堆积。
     */
    @Override
    public void logout(String accessToken) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            String jti = jwtTokenProvider.getJti(accessToken);
            long remaining = jwtTokenProvider.getAccessTokenExpiration();
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti, "1", remaining, TimeUnit.SECONDS);
            log.debug("Token已加入黑名单: jti={}", jti);
        }
    }

    // ==================== 私有辅助方法 ====================

    /** 签发 Token 对并存储 Refresh Token */
    private LoginResponse buildLoginResponse(String subject, String role,
                                              LoginResponse.UserInfo userInfo) {
        String accessToken = jwtTokenProvider.generateAccessToken(subject, role);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // Refresh Token 存储到 Redis，7天过期
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshToken,
                subject,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.SECONDS
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .userInfo(userInfo)
                .build();
    }

    /** 构建学生信息（用于响应） */
    private LoginResponse.UserInfo buildStudentUserInfo(Student student) {
        return LoginResponse.UserInfo.builder()
                .userId(student.getId())
                .studentNo(student.getStudentNo())
                .name(student.getName())
                .build();
    }

    /** 构建学生信息VO（脱敏手机号） */
    private StudentProfileVO buildProfileVO(Student student) {
        return StudentProfileVO.builder()
                .studentId(student.getId())
                .studentNo(student.getStudentNo())
                .name(student.getName())
                .phone(maskPhone(student.getPhone()))
                .violationCount(student.getViolationCount())
                .bannedUntil(student.getBannedUntil())
                .canReserve(student.getBannedUntil() == null ||
                        student.getBannedUntil().isBefore(java.time.LocalDateTime.now()))
                .todayReservationCount(0) // 由调用方填充
                .build();
    }

    /** 手机号脱敏：13812345678 → 138****5678 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
