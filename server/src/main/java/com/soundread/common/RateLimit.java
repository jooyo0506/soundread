package com.soundread.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解 — 基于用户维度的滑动窗口限流
 *
 * <p>
 * 使用 Caffeine 本地缓存实现，适用于单实例部署。
 * 标注在 Controller 方法上，自动按 userId + methodName 维度限流。
 * </p>
 *
 * <pre>
 * {@code @RateLimit(maxRequests = 5, windowSeconds = 60)}
 * public Result<?> chat(...) { ... }
 * </pre>
 *
 * @see com.soundread.common.aspect.RateLimitAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** 时间窗口内允许的最大请求次数 */
    int maxRequests() default 10;

    /** 时间窗口长度（秒） */
    int windowSeconds() default 60;

    /** 超限时返回的提示消息 */
    String message() default "操作过于频繁，请稍后再试";
}
