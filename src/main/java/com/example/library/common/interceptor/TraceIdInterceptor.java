package com.example.library.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 请求追踪ID拦截器 — 为每个HTTP请求生成唯一的 traceId。
 *
 * <p>流程：
 * <ol>
 *   <li>优先从请求头 X-Request-Id 获取（支持客户端传入）</li>
 *   <li>若不存在则生成 UUID</li>
 *   <li>放入 MDC，后续日志自动携带 traceId</li>
 *   <li>设置响应头 X-Request-Id，方便前端排查</li>
 *   <li>请求结束后清理 MDC，防止内存泄漏</li>
 * </ol>
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // 优先使用客户端传入的 traceId，否则生成新的
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后清理 MDC，避免线程池复用时 traceId 残留
        MDC.remove(TRACE_ID_KEY);
    }
}
