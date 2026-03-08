package com.soundread.sdk.podcast.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 播客响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodcastResponse {

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 音频数据
     */
    private byte[] audioData;

    /**
     * 音频URL
     */
    private String audioUrl;

    /**
     * 音频时长 (秒)
     */
    private Integer duration;

    /**
     * 文本长度
     */
    private Integer textLength;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 对话轮次列表
     */
    private List<DialogueRound> rounds;

    /**
     * 对话轮次
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DialogueRound {
        /**
         * 轮次ID
         */
        private int roundId;

        /**
         * 主播
         */
        private String speaker;

        /**
         * 内容
         */
        private String content;

        /**
         * 音频数据
         */
        private byte[] audioData;

        /**
         * 开始时间 (毫秒)
         */
        private long startTime;

        /**
         * 结束时间 (毫秒)
         */
        private long endTime;
    }

    /**
     * 创建成功响应
     */
    public static PodcastResponse success(byte[] audioData, int duration) {
        return PodcastResponse.builder()
                .success(true)
                .audioData(audioData)
                .duration(duration)
                .build();
    }

    /**
     * 创建带对话的响应
     */
    public static PodcastResponse success(byte[] audioData, int duration, List<DialogueRound> rounds) {
        return PodcastResponse.builder()
                .success(true)
                .audioData(audioData)
                .duration(duration)
                .rounds(rounds)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static PodcastResponse error(String errorCode, String errorMessage) {
        return PodcastResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
