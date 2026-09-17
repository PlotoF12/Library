package com.example.library.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

/**
 * 统一API响应体 — 所有Controller返回此类包装后的结果。
 *
 * <p>设计要点：
 * <ul>
 *   <li>code=0 表示业务成功，非0表示异常</li>
 *   <li>timestamp 用于问题排查时定位时间线</li>
 *   <li>requestId 从 MDC 中获取 traceId，实现全链路追踪</li>
 *   <li>@JsonInclude(NON_NULL) 避免返回 null 字段</li>
 * </ul>
 *
 * @param <T> 响应数据的类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** 业务状态码，0 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 服务端时间戳（毫秒） */
    private long timestamp;

    /** 请求追踪ID，与 MDC 中的 traceId 一致 */
    private String requestId;

    // ==================== 成功响应工厂方法 ====================

    /** 成功响应（无消息），用于查询类接口 */
    public static <T> ApiResponse<T> success(T data) {
        return build(0, "success", data);
    }

    /** 成功响应（自定义消息），用于创建/操作类接口 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return build(0, message, data);
    }

    // ==================== 失败响应工厂方法 ====================

    /** 失败响应（仅错误码和消息），用于参数校验/业务异常 */
    public static <T> ApiResponse<T> error(int code, String message) {
        return build(code, message, null);
    }

    /** 失败响应（携带数据），用于需要返回部分数据的异常场景 */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return build(code, message, data);
    }

    // ==================== 内部构建方法 ====================

    /**
     * 统一构建 ApiResponse，自动填充 timestamp 和 traceId。
     * traceId 从 MDC 中获取，如果为空则使用 "N/A"。
     */
    private static <T> ApiResponse<T> build(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .requestId(MDC.get("traceId") != null ? MDC.get("traceId") : "N/A")
                .build();
    }
}
