package com.soundread.common.exception;

import com.soundread.common.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 自定义业务异常
 *
 * <p>
 * 用于 Service 层在遇到可预见的业务规则违反时主动抛出，
 * 由 {@link GlobalExceptionHandler} 统一捕获并转为标准响应。
 * </p>
 *
 * <p>
 * 使用示例:
 * </p>
 * 
 * <pre>
 * // 自定义消息 (默认 code=400)
 * throw new BusinessException("用户不存在");
 *
 * // 使用状态码枚举
 * throw new BusinessException(ResultCode.FORBIDDEN, "VIP 专属功能");
 * </pre>
 *
 * @author SoundRead
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final int code;

    /**
     * 通用业务异常（默认 400）
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST.getCode();
    }

    /**
     * 指定错误码的业务异常
     *
     * @param code    HTTP 状态码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用 ResultCode 枚举构造业务异常
     *
     * @param resultCode 状态码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用 ResultCode 枚举 + 自定义消息构造业务异常
     *
     * @param resultCode 状态码枚举
     * @param message    自定义错误消息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
