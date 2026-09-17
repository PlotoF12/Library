package com.example.library.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 幂等性工具 — 基于 Redis SETNX 实现请求去重。
 *
 * <p>使用场景：
 * <ul>
 *   <li>创建预约接口：防止用户重复提交</li>
 *   <li>所有 POST 创建类接口均可使用</li>
 * </ul>
 *
 * <p>流程：
 * <ol>
 *   <li>客户端生成 UUID 作为 Idempotency-Key，放入请求头</li>
 *   <li>服务端收到请求后，先 SETNX 将 Key 写入 Redis</li>
 *   <li>写入成功 → 首次请求，继续执行</li>
 *   <li>写入失败 → 重复请求，返回已有结果</li>
 *   <li>Key 设置 TTL（默认1小时），自动过期清理</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class IdempotencyUtil {

    private final RedisTemplate<String, String> redisTemplate;

    /** Redis Key 前缀 */
    private static final String IDEMPOTENT_PREFIX = "idempotent:";

    /** 幂等键的默认过期时间（小时） */
    private static final long TTL_HOURS = 1;

    /**
     * 检查并设置幂等键。
     *
     * @param key   幂等键（由客户端传入的 Idempotency-Key）
     * @param value 要缓存的值（如 reservationId 的 JSON）
     * @return true 表示首次请求，false 表示重复请求
     */
    public boolean checkAndSet(String key, String value) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(buildKey(key), value, TTL_HOURS, TimeUnit.HOURS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 获取幂等键对应的缓存值。
     * 如果 Key 存在，返回之前缓存的结果；否则返回 null。
     */
    public String get(String idempotencyKey) {
        return redisTemplate.opsForValue().get(buildKey(idempotencyKey));
    }

    /**
     * 构建 Redis Key。
     * 格式：idempotent:{idempotencyKey}
     */
    public static String buildKey(String idempotencyKey) {
        return IDEMPOTENT_PREFIX + idempotencyKey;
    }
}
