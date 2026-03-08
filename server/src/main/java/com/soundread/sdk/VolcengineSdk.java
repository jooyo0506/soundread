package com.soundread.sdk;

import com.soundread.sdk.podcast.PodcastClient;
import com.soundread.sdk.tts.Tts1Client;
import com.soundread.sdk.tts.model.TtsResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 火山引擎SDK统一入口
 *
 * 使用示例:
 *
 * // 1. 创建SDK实例
 * VolcengineSdk sdk = VolcengineSdk.builder()
 * .appId("your-app-id")
 * .accessToken("your-access-token")
 * .build();
 *
 * // 2. TTS 1.0 短文本合成
 * byte[] audio = sdk.tts1().synthesize("你好世界", "BV001_streaming");
 *
 * // 3. TTS 1.0 长文本异步
 * String taskId = sdk.tts1().createLongTextTask(text, voiceId);
 * TtsResponse response = sdk.tts1().queryLongTextTask(taskId);
 *
 * // 4. TTS 2.0 流式合成
 * byte[] audio = sdk.tts2().synthesize("你好", "BV001_streaming");
 *
 * // 5. AI播客生成
 * byte[] audio = sdk.podcast().generate(text, voiceA, voiceB);
 *
 */
@Slf4j
public class VolcengineSdk {

    private final String appId;
    private final String accessToken;
    private final String cluster;
    private Tts1Client tts1Client;
    private PodcastClient podcastClient;

    private VolcengineSdk(Builder builder) {
        this.appId = builder.appId;
        this.accessToken = builder.accessToken;
        this.cluster = builder.cluster;
    }

    /**
     * 创建Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取TTS 1.0客户端
     */
    public Tts1Client tts1() {
        if (tts1Client == null) {
            tts1Client = new Tts1Client(appId, accessToken, cluster);
        }
        return tts1Client;
    }

    /**
     * 获取播客客户端
     */
    public PodcastClient podcast() {
        if (podcastClient == null) {
            podcastClient = new PodcastClient(appId, accessToken);
        }
        return podcastClient;
    }

    // ========== 便捷静态方法 ==========

    /**
     * 短文本合成 (TTS 1.0)
     */
    public static byte[] synthesizeShortText(String text, String voiceId, String appId, String accessToken) {
        return new Tts1Client(appId, accessToken, "volcano_tts").synthesize(text, voiceId);
    }

    /**
     * 长文本异步合成 (TTS 1.0)
     */
    public static String createLongTextTask(String text, String voiceId, String appId, String accessToken) {
        return new Tts1Client(appId, accessToken, "volcano_tts").createLongTextTask(text, voiceId);
    }

    /**
     * 查询长文本任务 (TTS 1.0)
     */
    public static TtsResponse queryLongTextTask(String taskId, String appId, String accessToken) {
        return new Tts1Client(appId, accessToken, "volcano_tts").queryLongTextTask(taskId);
    }

    /**
     * AI播客生成 (流式，请通过 WebSocket /ws/podcast 使用)
     * 
     * @deprecated 播客已改为流式生成，请使用 PodcastWebSocketHandler
     */
    @Deprecated
    public static byte[] generatePodcast(String text, String voiceA, String voiceB, String appId, String accessToken) {
        log.warn("generatePodcast 同步方法已废弃，请使用 WebSocket /ws/podcast 流式接口");
        return new byte[0];
    }

    // ========== Builder ==========

    public static class Builder {
        private String appId;
        private String accessToken;
        private String cluster = "volcano_tts";

        private Builder() {
        }

        /**
         * 设置App ID
         */
        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        /**
         * 设置Access Token
         */
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        /**
         * 设置集群
         */
        public Builder cluster(String cluster) {
            this.cluster = cluster;
            return this;
        }

        /**
         * 构建SDK实例
         */
        public VolcengineSdk build() {
            if (appId == null || appId.isEmpty()) {
                throw new IllegalArgumentException("appId is required");
            }
            if (accessToken == null || accessToken.isEmpty()) {
                throw new IllegalArgumentException("accessToken is required");
            }
            return new VolcengineSdk(this);
        }
    }
}
