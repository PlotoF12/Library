package com.example.library.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token 提供者 — 负责 Access Token 和 Refresh Token 的生成、解析与校验。
 *
 * <p>Token 类型：
 * <ul>
 *   <li><b>Access Token</b>：JWT 格式，有效期2小时，携带用户身份和角色，每次请求携带</li>
 *   <li><b>Refresh Token</b>：UUID 格式，有效期7天，存储于 Redis，仅用于刷新 Access Token</li>
 * </ul>
 *
 * <p>JWT Payload 结构：
 * <pre>
 * {
 *   "sub": "student_10001",    // 主体标识
 *   "role": "STUDENT",         // 角色
 *   "iat": 1718964000,         // 签发时间
 *   "exp": 1718971200,         // 过期时间
 *   "jti": "uuid"              // Token唯一ID（用于登出黑名单）
 * }
 * </pre>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        // 将 Base64 编码的密钥解码为 SecretKey
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * 生成 Access Token（JWT）。
     *
     * @param subject 主体标识，格式："student_{id}" 或 "admin_{id}"
     * @param role    角色：STUDENT 或 ADMIN
     * @return JWT 字符串
     */
    public String generateAccessToken(String subject, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration * 1000);

        return Jwts.builder()
                .subject(subject)           // "student_10001" 或 "admin_1"
                .claim("role", role)        // STUDENT 或 ADMIN
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())  // jti，用于登出黑名单
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 Refresh Token（UUID）。
     * Refresh Token 不携带用户信息，安全性更高。
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * 解析 JWT Token 并返回 Claims。
     *
     * @throws ExpiredJwtException       Token已过期
     * @throws MalformedJwtException     Token格式错误
     * @throws SignatureException        签名不匹配
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 Token 是否有效。
     * 返回 false 的情况：过期、格式错误、签名不匹配、不支持的Token类型。
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT签名无效: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT为空: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 Token 中提取主体标识（subject）。
     * 返回值示例："student_10001"、"admin_1"
     */
    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中提取角色。
     * 返回值："STUDENT" 或 "ADMIN"
     */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 从 Token 中提取 JTI（Token唯一ID）。
     * 用于登出时将 JTI 加入黑名单。
     */
    public String getJti(String token) {
        return parseToken(token).getId();
    }

    /** 获取 Access Token 过期时间（秒） */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /** 获取 Refresh Token 过期时间（秒） */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
