package com.soundread.sdk.podcast.builder;

import com.soundread.sdk.podcast.model.PodcastRequest;

import java.util.UUID;

/**
 * 播客请求构建器
 */
public class PodcastRequestBuilder {

    private String text;
    private String voiceA;
    private String voiceB;
    private boolean useHeadMusic = true;
    private boolean useTailMusic = false;
    private String userId;

    private PodcastRequestBuilder() {}

    /**
     * 创建新的构建器
     */
    public static PodcastRequestBuilder builder() {
        return new PodcastRequestBuilder();
    }

    /**
     * 设置文本
     */
    public PodcastRequestBuilder text(String text) {
        this.text = text;
        return this;
    }

    /**
     * 设置主播A音色
     */
    public PodcastRequestBuilder voiceA(String voiceA) {
        this.voiceA = voiceA;
        return this;
    }

    /**
     * 设置主播B音色
     */
    public PodcastRequestBuilder voiceB(String voiceB) {
        this.voiceB = voiceB;
        return this;
    }

    /**
     * 设置双主播音色 (使用预设组合)
     */
    public PodcastRequestBuilder voicePreset(VoicePreset preset) {
        this.voiceA = preset.getVoiceA();
        this.voiceB = preset.getVoiceB();
        return this;
    }

    /**
     * 设置开头音乐
     */
    public PodcastRequestBuilder useHeadMusic(boolean use) {
        this.useHeadMusic = use;
        return this;
    }

    /**
     * 设置结尾音乐
     */
    public PodcastRequestBuilder useTailMusic(boolean use) {
        this.useTailMusic = use;
        return this;
    }

    /**
     * 设置用户ID
     */
    public PodcastRequestBuilder userId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * 构建请求
     */
    public PodcastRequest build() {
        String requestId = UUID.randomUUID().toString();

        return PodcastRequest.builder()
                .text(text)
                .voiceA(voiceA)
                .voiceB(voiceB)
                .useHeadMusic(useHeadMusic)
                .useTailMusic(useTailMusic)
                .userId(userId)
                .requestId(requestId)
                .build();
    }

    // ========== 便捷方法 ==========

    /**
     * 创建简单播客请求
     */
    public static PodcastRequest create(String text, String voiceA, String voiceB) {
        return builder()
                .text(text)
                .voiceA(voiceA)
                .voiceB(voiceB)
                .build();
    }

    /**
     * 使用预设组合创建播客请求
     */
    public static PodcastRequest createWithPreset(String text, VoicePreset preset) {
        return builder()
                .text(text)
                .voicePreset(preset)
                .build();
    }

    /**
     * 预设音色组合
     */
    public enum VoicePreset {
        /**
         * 黑猫侦探社系列 - 咪仔 & 大一先生
         */
        DETECTIVE("zh_female_mizaitongxue_v2_saturn_bigtts",
                  "zh_male_dayixiansheng_v2_saturn_bigtts"),

        /**
         * 科技商业脱口秀 - 刘飞 & 潇磊
         */
        TECH("zh_male_liufei_v2_saturn_bigtts",
             "zh_male_xiaolei_v2_saturn_bigtts"),

        /**
         * 知识科普系列 - 小涵 & 博士
         */
        EDUCATION("zh_female_xiaohan_v2_saturn_bigtts",
                  "zh_male_boshi_v2_saturn_bigtts"),

        /**
         * 情感故事系列 - 小雅 & 阿文
         */
        STORY("zh_female_xiaoya_v2_saturn_bigtts",
               "zh_male_awen_v2_saturn_bigtts");

        private final String voiceA;
        private final String voiceB;

        VoicePreset(String voiceA, String voiceB) {
            this.voiceA = voiceA;
            this.voiceB = voiceB;
        }

        public String getVoiceA() {
            return voiceA;
        }

        public String getVoiceB() {
            return voiceB;
        }
    }
}
