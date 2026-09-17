package com.example.library.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置 — Key 和 Value 均使用 String 序列化。
 *
 * <p>为什么不使用 GenericJackson2JsonRedisSerializer：
 * 该序列化器在写入时会嵌入 @class 类型元数据，反序列化时依赖 Jackson 的无参构造器。
 * Lombok 的 @Builder 会移除默认无参构造器，导致反序列化失败（表现为首次请求成功、
 * 从 Redis 读取缓存后 500 错误）。
 *
 * <p>改用 StringRedisSerializer 后，所有缓存值均为 String 类型，简单可靠。
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
