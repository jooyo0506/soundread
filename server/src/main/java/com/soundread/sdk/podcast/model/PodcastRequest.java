package com.soundread.sdk.podcast.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 播客请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodcastRequest {

    /**
     * 输入文本
     */
    private String text;

    /**
     * 主播A音色ID
     */
    private String voiceA;

    /**
     * 主播B音色ID
     */
    private String voiceB;

    /**
     * 是否包含开头音乐
     */
    @Builder.Default
    private boolean useHeadMusic = true;

    /**
     * 是否包含结尾音乐
     */
    @Builder.Default
    private boolean useTailMusic = false;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 验证请求参数
     */
    public void validate() {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text is required");
        }
        if (voiceA == null || voiceA.isEmpty()) {
            throw new IllegalArgumentException("VoiceA is required");
        }
        if (voiceB == null || voiceB.isEmpty()) {
            throw new IllegalArgumentException("VoiceB is required");
        }
    }
}
