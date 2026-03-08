package com.soundread.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 播客请求 DTO
 */
public class PodcastDto {

    @Data
    public static class GenerateRequest {
        /** topic / url / text */
        @NotBlank
        private String sourceType;
        @NotBlank
        private String content;
        @NotBlank
        private String voiceA;
        @NotBlank
        private String voiceB;
        private Boolean useHeadMusic;
        private Boolean useTailMusic;
    }

    @Data
    public static class PresetResponse {
        private String name;
        private String description;
        private VoiceInfo voiceA;
        private VoiceInfo voiceB;
    }

    @Data
    public static class VoiceInfo {
        private String voiceId;
        private String name;
        private String gender;
    }
}
