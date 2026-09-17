package com.example.library.common.config;

import com.example.library.common.interceptor.RateLimitInterceptor;
import com.example.library.common.interceptor.TraceIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 — 注册全局拦截器。
 *
 * <p>拦截器执行顺序（按注册顺序）：
 * <ol>
 *   <li>TraceIdInterceptor — 先生成 traceId，后续所有日志和响应都携带</li>
 *   <li>RateLimitInterceptor — 再检查限流，避免被限流的请求也消耗 traceId 之外的资源</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdInterceptor traceIdInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 追踪ID拦截器 — 拦截所有请求（包括静态资源，方便排查）
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**")
                .order(1);

        // 限流拦截器 — 只拦截 API 请求
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(2);
    }
}
