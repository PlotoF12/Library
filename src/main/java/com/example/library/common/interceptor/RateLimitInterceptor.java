package com.example.library.common.interceptor;

import com.example.library.common.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器 — 基于 Redis ZSET 滑动窗口算法。
 *
 * <p>流程：
 * <ol>
 *   <li>拦截所有请求，检查 HandlerMethod 是否有 @RateLimit 注解</li>
 *   <li>无注解 → 放行</li>
 *   <li>有注解 → 以 userId + key 构建 Redis Key</li>
 *   <li>使用 ZSET 存储请求时间戳，移除窗口外的记录</li>
 *   <li>统计窗口内请求数，超过 limit 返回 429</li>
 * </ol>
 *
 * <p>为什么用 ZSET 而非简单计数器：
 * ZSET 可以实现真正的滑动窗口，而非固定窗口。固定窗口在边界时刻（如12:00:59和12:01:00）
 * 可能被绕过，ZSET 的滑动窗口完全避免了这个问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;

    /** 限流 Key 前缀 */
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 非 Controller 方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查是否有 @RateLimit 注解
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        // 构建限流 Key：ratelimit:{userId}:{key}
        // userId 从 SecurityContext 获取，未登录用户使用 IP
        String userId = getUserId(request);
        String redisKey = RATE_LIMIT_PREFIX + userId + ":" + rateLimit.key();

        boolean allowed = checkAndIncr(redisKey, rateLimit);
        if (!allowed) {
            log.warn("限流触发: userId={}, key={}, limit={}/{}",
                    userId, rateLimit.key(), rateLimit.limit(), rateLimit.window());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":10005,\"message\":\""
                    + rateLimit.message() + "\",\"data\":null}");
            return false;
        }
        return true;
    }

    /**
     * 滑动窗口限流核心算法。
     *
     * <p>使用 Redis ZSET：
     * <ul>
     *   <li>Score = 请求时间戳（毫秒）</li>
     *   <li>Member = 时间戳 + 随机数（唯一标识每次请求）</li>
     *   <li>先移除窗口外的记录（ZREMRANGEBYSCORE）</li>
     *   <li>统计窗口内记录数（ZCARD）</li>
     *   <li>未超限则添加当前时间戳（ZADD）并设置 Key 过期时间</li>
     * </ul>
     */
    private boolean checkAndIncr(String key, RateLimit rateLimit) {
        long now = System.currentTimeMillis();
        long windowStart = now - rateLimit.timeUnit().toMillis(rateLimit.window());
        String member = now + ":" + Thread.currentThread().getId();

        // 移除窗口外的记录
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        // 统计窗口内请求数
        Long count = redisTemplate.opsForZSet().zCard(key);

        if (count != null && count >= rateLimit.limit()) {
            return false; // 超限
        }

        // 未超限，添加当前记录并设置过期时间
        redisTemplate.opsForZSet().add(key, member, now);
        redisTemplate.expire(key, rateLimit.window() + 10, rateLimit.timeUnit());
        return true;
    }

    /**
     * 获取当前用户标识。
     * 优先从 SecurityContext 获取 userId，未登录时使用 IP 地址。
     */
    private String getUserId(HttpServletRequest request) {
        // 从 SecurityContextHolder 获取，若不存在则使用 IP
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return "user:" + userId;
        }
        return "ip:" + request.getRemoteAddr();
    }
}
