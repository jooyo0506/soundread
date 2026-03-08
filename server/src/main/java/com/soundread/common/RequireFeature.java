package com.soundread.common;

import java.lang.annotation.*;

/**
 * 功能权限校验注解
 * <p>
 * 标注在 Controller 方法上, 进入方法前自动校验当前用户的
 * tier_code 是否拥有该功能的 feature_flag 开关。
 * 若无权限, 将抛出 BusinessException(403, "当前等级无法使用此功能")
 * </p>
 *
 * @example
 * 
 *          <pre>
 * {@code @RequireFeature("ai_podcast")}
 * {@code @PostMapping("/generate")}
 * public Result<?> generatePodcast(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFeature {

    /**
     * 功能标识, 对应 feature_flags JSON 中的 key
     * 例如: "tts_basic", "ai_podcast", "voice_clone", "tts_emotion_v2"
     */
    String value();

    /**
     * 无权限时的自定义提示信息
     */
    String message() default "当前等级无法使用此功能，请升级会员";
}
