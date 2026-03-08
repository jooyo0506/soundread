package com.soundread.sdk.tts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequest {

    /**
     * 文本内容
     */
    private String text;

    /**
     * 音色ID
     */
    private String voiceId;

    /**
     * 语速: 0.5-2.0, 默认1.0
     */
    @Builder.Default
    private Float speedRatio = 1.0f;

    /**
     * 音量: 0.5-2.0, 默认1.0
     */
    @Builder.Default
    private Float volumeRatio = 1.0f;

    /**
     * 音调: 0.5-2.0, 默认1.0
     */
    @Builder.Default
    private Float pitchRatio = 1.0f;

    /**
     * 音频编码: mp3, wav, pcm
     */
    @Builder.Default
    private String encoding = "mp3";

    /**
     * 采样率: 8000, 16000, 24000
     */
    @Builder.Default
    private Integer sampleRate = 24000;

    /**
     * 用户ID (用于追踪)
     */
    private String userId;

    /**
     * 请求ID (用于追踪)
     */
    private String requestId;

    /**
     * TTS类型: SHORT, LONG, EMOTION
     */
    private TtsType type;

    /**
     * TTS类型枚举
     */
    public enum TtsType {
        /**
         * 短文本合成
         */
        SHORT,
        /**
         * 长文本合成
         */
        LONG,
        /**
         * 情感合成
         */
        EMOTION
    }

    /**
     * 验证请求参数
     */
    public void validate() {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text is required");
        }
        if (voiceId == null || voiceId.isEmpty()) {
            throw new IllegalArgumentException("VoiceId is required");
        }
        if (speedRatio < 0.5f || speedRatio > 2.0f) {
            throw new IllegalArgumentException("SpeedRatio must be between 0.5 and 2.0");
        }
        if (volumeRatio < 0.5f || volumeRatio > 2.0f) {
            throw new IllegalArgumentException("VolumeRatio must be between 0.5 and 2.0");
        }
        if (pitchRatio < 0.5f || pitchRatio > 2.0f) {
            throw new IllegalArgumentException("PitchRatio must be between 0.5 and 2.0");
        }
    }

    // ========== 便捷静态工厂方法 ==========

    /**
     * 创建短文本合成请求
     */
    public static TtsRequest createShortText(String text, String voiceId) {
        TtsRequest request = new TtsRequest();
        request.text = text;
        request.voiceId = voiceId;
        request.type = TtsType.SHORT;
        request.speedRatio = 1.0f;
        request.volumeRatio = 1.0f;
        request.pitchRatio = 1.0f;
        request.encoding = "mp3";
        request.sampleRate = 24000;
        return request;
    }

    /**
     * 创建长文本合成请求
     */
    public static TtsRequest createLongText(String text, String voiceId) {
        TtsRequest request = new TtsRequest();
        request.text = text;
        request.voiceId = voiceId;
        request.type = TtsType.LONG;
        return request;
    }

    /**
     * 创建情感合成请求
     */
    public static TtsRequest createEmotion(String text, String voiceId) {
        TtsRequest request = new TtsRequest();
        request.text = text;
        request.voiceId = voiceId;
        request.type = TtsType.EMOTION;
        return request;
    }
}
