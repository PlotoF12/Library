package com.example.library.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置 — 为短信通知等非实时操作提供线程池。
 *
 * <p>线程池参数说明：
 * <ul>
 *   <li>corePoolSize=2：日常最少保持2个线程</li>
 *   <li>maxPoolSize=5：峰值最多5个线程</li>
 *   <li>queueCapacity=200：缓冲队列200个任务</li>
 *   <li>CallerRunsPolicy：队列满时由调用线程执行，不会丢任务</li>
 * </ul>
 *
 * <p>适用场景：短信发送(@Async("notificationExecutor"))、操作日志记录等。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
