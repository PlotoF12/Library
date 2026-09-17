package com.example.library.infrastructure;

import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置 — 提供分布式锁、延迟队列、布隆过滤器等高级功能。
 *
 * <p>三个核心 Bean：
 * <ul>
 *   <li><b>RedissonClient</b>：底层客户端，分布式锁 RLock 的入口</li>
 *   <li><b>RBloomFilter</b>：座位ID布隆过滤器，防缓存穿透（查询不存在的座位ID绕过缓存直接打DB）</li>
 *   <li><b>RDelayedQueue</b>：爽约延迟队列，预约开始15分钟后精确触发检查</li>
 * </ul>
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    /** Bloom Filter 期望插入的元素数量（座位总数上限） */
    private static final long BLOOM_EXPECTED_INSERTIONS = 5000L;

    /** Bloom Filter 期望误判率（3% 在性能和内存之间取得平衡） */
    private static final double BLOOM_FALSE_PROBABILITY = 0.03;

    /**
     * RedissonClient — 单节点模式连接。
     * destroyMethod="shutdown" 确保应用关闭时释放连接。
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password.isBlank() ? null : password)
                .setConnectionPoolSize(16)
                .setConnectionMinimumIdleSize(4);
        return Redisson.create(config);
    }

    /**
     * 座位ID布隆过滤器 — 防缓存穿透。
     *
     * <p>作用：查询座位时，先过布隆过滤器。
     * 如果布隆过滤器返回"不存在"，则座位ID一定不存在，直接返回404，无需查 Redis/DB。
     * 如果返回"可能存在"，再继续查 Redis → DB。
     *
     * <p>初始化后需要在 SeatService 中加载全部座位ID到布隆过滤器。
     */
    @Bean
    public RBloomFilter<Long> seatBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("bloom:seat:ids");
        bloomFilter.tryInit(BLOOM_EXPECTED_INSERTIONS, BLOOM_FALSE_PROBABILITY);
        return bloomFilter;
    }

    /**
     * 爽约检查延迟队列 — 预约创建时投递，15分钟后触发检查。
     *
     * <p>为什么用延迟队列而非 @Scheduled 扫描：
     * - @Scheduled 每秒扫描全表：浪费数据库资源
     * - 延迟队列：预约创建时投递一个延迟消息，15分钟后精确触发，只检查这一条记录
     * - 两者结合：延迟队列为主要方案，@Scheduled 每秒扫描为兜底
     */
    @Bean
    public RDelayedQueue<Long> noShowDelayedQueue(RedissonClient redissonClient) {
        RBlockingQueue<Long> blockingQueue =
                redissonClient.getBlockingQueue("delay:no_show_check");
        return redissonClient.getDelayedQueue(blockingQueue);
    }

    /**
     * 爽约检查阻塞队列 — 供延迟队列消费者使用。
     * Redisson 的延迟消息到期后自动转移到关联的 RBlockingQueue。
     */
    @Bean
    public RBlockingQueue<Long> noShowBlockingQueue(RedissonClient redissonClient) {
        return redissonClient.getBlockingQueue("delay:no_show_check");
    }
}
