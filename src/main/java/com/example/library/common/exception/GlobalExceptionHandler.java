package com.example.library.common.exception;

import com.example.library.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 统一将各类异常转换为 ApiResponse 格式返回。
 *
 * <p>处理优先级：
 * <ol>
 *   <li>业务异常 BizException → 返回对应的业务错误码和消息</li>
 *   <li>参数校验异常 → 400 + 拼接校验失败字段</li>
 *   <li>唯一约束冲突 DuplicateKeyException → 409（并发预约防重关键）</li>
 *   <li>权限不足 AccessDeniedException → 403</li>
 *   <li>其他未捕获异常 → 500（记录完整堆栈）</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 通用错误码 */
    private static final int CODE_PARAM_ERROR = 10001;
    private static final int CODE_FORBIDDEN = 10003;
    private static final int CODE_SERVER_ERROR = 500;

    /**
     * 业务异常处理 — 返回预设的业务错误码和消息。
     * 例如：座位已预约(20006)、惩罚期未过(20003) 等
     */
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理 — 收集所有校验失败的字段信息，拼接为可读字符串。
     * 处理 @Valid 注解触发的 MethodArgumentNotValidException 和 BindException。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(Exception e) {
        String errorMsg;
        if (e instanceof MethodArgumentNotValidException ex) {
            errorMsg = ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        } else if (e instanceof BindException ex) {
            errorMsg = ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        } else {
            errorMsg = "参数校验失败";
        }
        log.debug("参数校验异常: {}", errorMsg);
        return ApiResponse.error(CODE_PARAM_ERROR, errorMsg);
    }

    /**
     * 唯一约束冲突处理 — 这是并发预约防重的关键兜底。
     * 多个请求同时 INSERT 同一座位同时段时，数据库 UNIQUE KEY 会拒绝其中一个，
     * 此处捕获 DuplicateKeyException 并返回 409 Conflict。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("唯一约束冲突（并发防重触发）: {}", e.getMessage());
        return ApiResponse.error(20006, "该座位此时段已被预约");
    }

    /**
     * 数据完整性异常处理 — 外键约束、非空约束等。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("数据完整性异常: {}", e.getMessage(), e);
        // 如果内部原因是 DuplicateKeyException，走并发冲突逻辑
        if (e.getCause() instanceof DuplicateKeyException) {
            return handleDuplicateKeyException((DuplicateKeyException) e.getCause());
        }
        return ApiResponse.error(10001, "数据操作失败，请检查输入");
    }

    /**
     * 权限不足处理 — Spring Security 拦截后抛出。
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return ApiResponse.error(CODE_FORBIDDEN, "权限不足，无法执行此操作");
    }

    /**
     * 兜底异常处理 — 捕获所有未被上述处理器捕获的异常。
     * 记录完整堆栈以便排查，但只向客户端返回通用错误信息，不暴露内部细节。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("未预期的服务器异常: {}", e.getMessage(), e);
        return ApiResponse.error(CODE_SERVER_ERROR, "服务器内部错误，请稍后重试");
    }
}
