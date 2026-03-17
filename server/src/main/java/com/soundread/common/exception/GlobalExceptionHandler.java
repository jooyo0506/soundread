package com.soundread.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.soundread.common.Result;
import com.soundread.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * <p>
 * 统一拦截 Controller 层抛出的各类异常，
 * 将其转换为标准的 {@link Result} 响应体返回给前端。
 * 遵循阿里巴巴规范：异常不要用来做流程控制，但对已知的业务异常需要有兜底处理。
 * </p>
 *
 * <p>
 * 异常处理优先级（从高到低）:
 * </p>
 * <ol>
 * <li>Sa-Token 认证异常（NotLoginException）</li>
 * <li>Sa-Token 角色异常（NotRoleException）</li>
 * <li>业务异常（BusinessException）</li>
 * <li>配额超限异常（QuotaExceededException）</li>
 * <li>参数校验异常（MethodArgumentNotValidException）</li>
 * <li>兜底：未知异常（Exception）</li>
 * </ol>
 *
 * @author SoundRead
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLogin(NotLoginException e) {
        return Result.fail(ResultCode.UNAUTHORIZED);
    }

    /**
     * Sa-Token 角色权限不足
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNotRole(NotRoleException e) {
        return Result.fail(ResultCode.FORBIDDEN, "该功能需要 VIP 会员权限");
    }

    /**
     * 自定义业务异常（由 Service 层主动抛出）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 配额超限异常（限流/额度不足）
     */
    @ExceptionHandler(QuotaExceededException.class)
    public Result<?> handleQuotaExceeded(QuotaExceededException e) {
        return Result.fail(ResultCode.QUOTA_EXCEEDED, e.getMessage());
    }

    /**
     * Spring Validation 参数校验异常
     *
     * <p>
     * 提取第一条校验错误信息返回给前端，避免暴露全部校验细节。
     * </p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String firstError = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.fail(ResultCode.BAD_REQUEST, firstError);
    }

    /**
     * 404 静态资源或接口未找到异常 (Spring Boot 3.2 默认抛出 NoResourceFoundException)
     * 避免因前端拼写错误（如 api/upwload）导致控制台疯狂打印大段未知异常的堆栈。
     */
    @ExceptionHandler({
            org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class
    })
    public Result<?> handleNotFoundException(Exception e) {
        log.warn("接口或资源不存在: {}", e.getMessage());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /**
     * 兜底异常处理 — 捕获所有未被上层拦截的未知异常
     *
     * <p>
     * 阿里巴巴规范：对于公司外的 HTTP/API 开放接口，
     * 必须使用错误码；而应用内部推荐异常抛出。
     * 此处作为最后防线，记录完整堆栈并返回友好提示。
     * </p>
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统未知异常: ", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}
