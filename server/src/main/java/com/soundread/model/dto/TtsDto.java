package com.soundread.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * TTS 合成请求 DTO
 */
public class TtsDto {

    @Data
    public static class ShortTextRequest {
        @NotBlank
        private String text;
        @NotBlank
        private String voiceId;
        private Float speed; // 语速 [0.2, 3.0]，默认1.0
        private Float volume; // 音量 [0.1, 3.0]，默认1.0
        private Float pitch; // 音高 [0.1, 3.0]，默认1.0
    }

    @Data
    public static class AiScriptRequest {
        @NotBlank(message = "场景描述不能为空")
        private String prompt;
    }

    @Data
    public static class EmotionSynthesizeRequest {
        @NotBlank
        private String text;
        @NotBlank
        private String voiceId;
        private String voiceInstruction; // 语音指令: "#开心", "#四川话" 等
        private String referenceText; // 引入上文 (情感上下文)
    }

    @Data
    public static class LongTextRequest {
        @NotBlank
        private String text;
        @NotBlank
        private String voiceId;
        private Boolean useEmotion;
    }

    @Data
    public static class SynthesizeResponse {
        private String audioUrl;
        private Integer duration;
    }

    @Data
    public static class TaskResponse {
        private String taskId;
        private String status;
        private String audioUrl;
        private Integer progress;
    }
}
