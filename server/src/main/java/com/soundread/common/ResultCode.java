package com.soundread.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 *
 * <p>
 * 遵循阿里巴巴 Java 开发手册：
 * 所有整型包装类对象之间值的比较，全部使用 equals 方法。
 * 禁止在代码中出现"魔法值"（未经定义的常量）。
 * </p>
 *
 * <p>
 * 编码规范:
 * - 200: 成功
 * - 400: 客户端请求参数错误 / 通用业务异常
 * - 401: 未认证（未登录）
 * - 403: 无权限
 * - 429: 配额超限（限流）
 * - 500: 服务器内部错误
 * </p>
 *
 * @author SoundRead
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "success"),

    /** 客户端请求参数错误 / 通用业务失败 */
    BAD_REQUEST(400, "请求参数错误"),

    /** 未登录 */
    UNAUTHORIZED(401, "请先登录"),

    /** 无权限访问 */
    FORBIDDEN(403, "无权限访问"),

    /** 接口不存在 */
    NOT_FOUND(404, "接口不存在"),

    /** 配额超限 */
    QUOTA_EXCEEDED(429, "配额已用完"),

    /** 服务器内部错误 */
    INTERNAL_ERROR(500, "服务器内部错误");

    /** HTTP 状态码 */
    private final int code;

    /** 默认提示信息 */
    private final String message;
}
