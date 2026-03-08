package com.soundread.sdk.tts.builder;

import com.soundread.sdk.tts.model.TtsRequest;

/**
 * TTS请求构建器
 */
public class TtsRequestBuilder {

    private String text;
    private String voiceId;
    private Float speedRatio = 1.0f;
    private Float volumeRatio = 1.0f;
    private Float pitchRatio = 1.0f;
    private String encoding = "mp3";
    private Integer sampleRate = 24000;
    private String userId;
    private TtsRequest.TtsType type = TtsRequest.TtsType.SHORT;

    private TtsRequestBuilder() {}

    /**
     * 创建新的构建器
     */
    public static TtsRequestBuilder builder() {
        return new TtsRequestBuilder();
    }

    /**
     * 设置文本
     */
    public TtsRequestBuilder text(String text) {
        this.text = text;
        return this;
    }

    /**
     * 设置音色ID
     */
    public TtsRequestBuilder voiceId(String voiceId) {
        this.voiceId = voiceId;
        return this;
    }

    /**
     * 使用预定义音色
     */
    public TtsRequestBuilder voice(com.soundread.sdk.tts.model.VoiceInfo voice) {
        this.voiceId = voice.getVoiceId();
        return this;
    }

    /**
     * 设置语速 (0.5-2.0)
     */
    public TtsRequestBuilder speed(float speedRatio) {
        this.speedRatio = speedRatio;
        return this;
    }

    /**
     * 设置音量 (0.5-2.0)
     */
    public TtsRequestBuilder volume(float volumeRatio) {
        this.volumeRatio = volumeRatio;
        return this;
    }

    /**
     * 设置音调 (0.5-2.0)
     */
    public TtsRequestBuilder pitch(float pitchRatio) {
        this.pitchRatio = pitchRatio;
        return this;
    }

    /**
     * 设置音频格式
     */
    public TtsRequestBuilder encoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    /**
     * 设置采样率
     */
    public TtsRequestBuilder sampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    /**
     * 设置用户ID
     */
    public TtsRequestBuilder userId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * 设置为短文本类型
     */
    public TtsRequestBuilder shortText() {
        this.type = TtsRequest.TtsType.SHORT;
        return this;
    }

    /**
     * 设置为长文本类型
     */
    public TtsRequestBuilder longText() {
        this.type = TtsRequest.TtsType.LONG;
        return this;
    }

    /**
     * 设置为情感合成类型
     */
    public TtsRequestBuilder emotion() {
        this.type = TtsRequest.TtsType.EMOTION;
        return this;
    }

    /**
     * 构建请求
     */
    public TtsRequest build() {
        TtsRequest request = new TtsRequest();
        request.setText(text);
        request.setVoiceId(voiceId);
        request.setSpeedRatio(speedRatio);
        request.setVolumeRatio(volumeRatio);
        request.setPitchRatio(pitchRatio);
        request.setEncoding(encoding);
        request.setSampleRate(sampleRate);
        request.setUserId(userId);
        request.setType(type);
        return request;
    }

    // ========== 便捷方法 ==========

    /**
     * 创建短文本合成请求
     */
    public static TtsRequest shortText(String text, String voiceId) {
        return TtsRequest.createShortText(text, voiceId);
    }

    /**
     * 创建长文本合成请求
     */
    public static TtsRequest longText(String text, String voiceId) {
        return TtsRequest.createLongText(text, voiceId);
    }

    /**
     * 创建情感合成请求
     */
    public static TtsRequest emotion(String text, String voiceId) {
        return TtsRequest.createEmotion(text, voiceId);
    }
}
