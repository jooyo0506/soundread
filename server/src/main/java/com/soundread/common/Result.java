package com.soundread.common;

import lombok.Data;

/**
 * 统一 API 响应包装体
 *
 * <p>
 * 所有 Controller 的返回值均使用此包装，
 * 保证前端收到的 JSON 结构一致：{ code, message, data }
 * </p>
 *
 * <p>
 * 使用示例:
 * </p>
 * 
 * <pre>
 * // 成功 (带数据)
 * return Result.ok(user);
 *
 * // 成功 (无数据)
 * return Result.ok();
 *
 * // 失败 (使用枚举)
 * return Result.fail(ResultCode.UNAUTHORIZED);
 *
 * // 失败 (自定义消息)
 * return Result.fail(ResultCode.BAD_REQUEST, "手机号格式错误");
 * </pre>
 *
 * @param <T> 响应数据的泛型类型
 * @author SoundRead
 */
@Data
public class Result<T> {

    /** 状态码 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    private Result() {
    }

    // ==================== 成功响应 ====================

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 包装后的成功响应
     */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 包装后的成功响应
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    // ==================== 失败响应 ====================

    /**
     * 失败响应（使用状态码枚举）
     *
     * @param resultCode 状态码枚举
     * @param <T>        数据类型
     * @return 包装后的失败响应
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> r = new Result<>();
        r.code = resultCode.getCode();
        r.message = resultCode.getMessage();
        return r;
    }

    /**
     * 失败响应（使用状态码枚举 + 自定义消息）
     *
     * @param resultCode 状态码枚举
     * @param message    自定义错误消息
     * @param <T>        数据类型
     * @return 包装后的失败响应
     */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        Result<T> r = new Result<>();
        r.code = resultCode.getCode();
        r.message = message;
        return r;
    }

    /**
     * 失败响应（指定 code + message）
     *
     * <p>
     * 兼容旧代码，新代码建议使用 {@link #fail(ResultCode)} 或 {@link #fail(ResultCode, String)}
     * </p>
     *
     * @param code    状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 包装后的失败响应
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    /**
     * 失败响应（仅消息，默认 500）
     *
     * <p>
     * 兼容旧代码，新代码建议使用 {@link #fail(ResultCode, String)}
     * </p>
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 包装后的失败响应
     */
    public static <T> Result<T> fail(String message) {
        return fail(ResultCode.INTERNAL_ERROR.getCode(), message);
    }
}
