package com.soundread.sdk.tts.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TTS SDK配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "volcengine.tts")
public class TtsProperties {

    /**
     * App ID
     */
    private String appId;

    /**
     * Access Token
     */
    private String accessToken;

    /**
     * 集群
     */
    private String cluster = "volcano_tts";

    /**
     * API端点
     */
    private String endpoint = "https://openspeech.bytedance.com/api/v1/tts";

    /**
     * 异步提交端点
     */
    private String asyncSubmitUrl = "https://openspeech.bytedance.com/api/v1/tts_async/submit";

    /**
     * 异步查询端点
     */
    private String asyncQueryUrl = "https://openspeech.bytedance.com/api/v1/tts_async/query";

    /**
     * 异步情感版提交端点
     */
    private String asyncEmotionSubmitUrl = "https://openspeech.bytedance.com/api/v1/tts_async_with_emotion/submit";

    /**
     * 异步情感版查询端点
     */
    private String asyncEmotionQueryUrl = "https://openspeech.bytedance.com/api/v1/tts_async_with_emotion/query";

    /**
     * 默认资源ID
     */
    private String resourceId = "volc.tts_async.default";

    /**
     * 情感版资源ID
     */
    private String emotionResourceId = "volc.tts_async.emotion";

    /**
     * 默认音色
     */
    private String defaultVoice = "BV001_streaming";

    /**
     * 默认音频格式
     */
    private String defaultEncoding = "mp3";

    /**
     * 默认采样率
     */
    private int defaultSampleRate = 24000;

    /**
     * 短文本最大字符数
     */
    private int shortTextMaxLength = 300;

    /**
     * 长文本最大字符数
     */
    private int longTextMaxLength = 100000;
}
