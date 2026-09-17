package com.example.library.infrastructure;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置 — Mapper扫描。
 * 分页通过 XML 动态 SQL 手动实现（LIMIT/OFFSET），无需额外插件。
 */
@Configuration
@MapperScan("com.example.library.module.**.mapper")
public class MyBatisPlusConfig {
}
