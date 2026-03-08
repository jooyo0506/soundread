package com.soundread.common.exception;

import java.io.Serial;

/**
 * 配额超限异常
 *
 * <p>
 * 当用户的日限额（文本字数 / AI 剧本次数 / 边听边问次数等）
 * 已耗尽时，由 {@link com.soundread.service.QuotaService} 抛出。
 * 由 {@link GlobalExceptionHandler} 统一捕获，返回 HTTP 429 状态码。
 * </p>
 *
 * @author SoundRead
 */
public class QuotaExceededException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 构造配额超限异常
     *
     * @param message 超限提示消息（会直接展示给前端用户）
     */
    public QuotaExceededException(String message) {
        super(message);
    }
}
