package com.example.library.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置 — 爽约扫描、超时释放
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
