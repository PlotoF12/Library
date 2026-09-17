package com.example.library.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器 — 每个请求拦截并验证 Token，设置安全上下文。
 *
 * <p>流程（每个请求执行一次）：
 * <ol>
 *   <li>从 Authorization 头提取 Bearer Token</li>
 *   <li>校验 Token 有效性（签名、过期）</li>
 *   <li>检查 Token 是否在黑名单中（Redis）— 已登出的 Token 不可用</li>
 *   <li>从 Token 中提取用户ID和角色</li>
 *   <li>设置 SecurityContextHolder，后续 @PreAuthorize 和 AuthenticationPrincipal 可用</li>
 * </ol>
 *
 * <p>如果 Token 无效或已登出，不设置 SecurityContext，由 SecurityConfig 的异常处理返回 401。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 校验 Token 有效性
        if (!jwtTokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 检查是否在黑名单中（已登出）
        String jti = jwtTokenProvider.getJti(token);
        String blacklisted = redisTemplate.opsForValue().get(BLACKLIST_PREFIX + jti);
        if (blacklisted != null) {
            log.debug("Token已被登出: jti={}", jti);
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取用户信息并设置 SecurityContext
        try {
            String subject = jwtTokenProvider.getSubject(token);  // "student_10001"
            String role = jwtTokenProvider.getRole(token);        // "STUDENT"

            // 从 subject 中提取数字ID：student_10001 → 10001, admin_1 → 1
            Long userId = Long.parseLong(subject.split("_")[1]);

            // 构建认证对象，将 userId 作为 Principal
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,                          // Principal → @AuthenticationPrincipal
                            null,
                            Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_" + role))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("Token解析失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取 Bearer Token。
     * 返回 null 表示请求未携带 Token。
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
