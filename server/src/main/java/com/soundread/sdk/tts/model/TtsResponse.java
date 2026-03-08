package com.soundread.sdk.tts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsResponse {

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 任务ID (长文本异步合成时返回)
     */
    private String taskId;

    /**
     * 音频数据 (短文本同步返回)
     */
    private byte[] audioData;

    /**
     * 音频URL (长文本异步返回)
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
     * 音频格式
     */
    private String format;

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
     * 任务状态 (长文本异步)
     */
    private TaskStatus taskStatus;

    /**
     * 字幕信息 (长文本)
     */
    private Subtitles subtitles;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        /**
         * 处理中
         */
        PROCESSING,
        /**
         * 成功
         */
        SUCCESS,
        /**
         * 失败
         */
        FAILED
    }

    /**
     * 字幕信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subtitles {
        private String[] sentences;
        private int[] startTimes;
        private int[] endTimes;
    }

    /**
     * 创建成功响应
     */
    public static TtsResponse success(byte[] audioData, int duration) {
        return TtsResponse.builder()
                .success(true)
                .audioData(audioData)
                .duration(duration)
                .build();
    }

    /**
     * 创建异步任务响应
     */
    public static TtsResponse taskCreated(String taskId, String audioUrl, int textLength) {
        return TtsResponse.builder()
                .success(true)
                .taskId(taskId)
                .audioUrl(audioUrl)
                .textLength(textLength)
                .taskStatus(TaskStatus.PROCESSING)
                .build();
    }

    /**
     * 创建任务完成响应
     */
    public static TtsResponse taskCompleted(String taskId, String audioUrl, int duration, Subtitles subtitles) {
        return TtsResponse.builder()
                .success(true)
                .taskId(taskId)
                .audioUrl(audioUrl)
                .duration(duration)
                .taskStatus(TaskStatus.SUCCESS)
                .subtitles(subtitles)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static TtsResponse error(String errorCode, String errorMessage) {
        return TtsResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .taskStatus(TaskStatus.FAILED)
                .build();
    }
}
