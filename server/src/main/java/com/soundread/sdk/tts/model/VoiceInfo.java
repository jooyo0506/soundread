package com.soundread.sdk.tts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音色信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceInfo {

    /**
     * 音色ID
     */
    private String voiceId;

    /**
     * 音色名称
     */
    private String name;

    /**
     * 语言区域
     */
    private String locale;

    /**
     * 性别: Male, Female
     */
    private String gender;

    /**
     * 简短描述
     */
    private String shortName;

    /**
     * 是否支持情感合成
     */
    private boolean supportEmotion;

    /**
     * 预定义的音色列表
     */
    public static final class PresetVoices {

        // 中文音色
        public static final VoiceInfo BV001 = VoiceInfo.builder()
                .voiceId("BV001_streaming")
                .name("通用女声")
                .locale("zh-CN")
                .gender("Female")
                .shortName("通用女声")
                .supportEmotion(false)
                .build();

        public static final VoiceInfo BV002 = VoiceInfo.builder()
                .voiceId("BV002_streaming")
                .name("通用男声")
                .locale("zh-CN")
                .gender("Male")
                .shortName("通用男声")
                .supportEmotion(false)
                .build();

        public static final VoiceInfo BV700 = VoiceInfo.builder()
                .voiceId("BV700_streaming")
                .name("灿灿")
                .locale("zh-CN")
                .gender("Female")
                .shortName("灿灿")
                .supportEmotion(false)
                .build();

        public static final VoiceInfo BV102 = VoiceInfo.builder()
                .voiceId("BV102_streaming")
                .name("儒雅青年")
                .locale("zh-CN")
                .gender("Male")
                .shortName("儒雅青年")
                .supportEmotion(false)
                .build();

        public static final VoiceInfo BV113 = VoiceInfo.builder()
                .voiceId("BV113_streaming")
                .name("甜宠少御")
                .locale("zh-CN")
                .gender("Female")
                .shortName("甜宠少御")
                .supportEmotion(false)
                .build();

        public static final VoiceInfo BV033 = VoiceInfo.builder()
                .voiceId("BV033_streaming")
                .name("温柔小哥")
                .locale("zh-CN")
                .gender("Male")
                .shortName("温柔小哥")
                .supportEmotion(false)
                .build();

        public static final VoiceInfo BV034 = VoiceInfo.builder()
                .voiceId("BV034_streaming")
                .name("知性姐姐")
                .locale("zh-CN")
                .gender("Female")
                .shortName("知性姐姐-双语")
                .supportEmotion(false)
                .build();

        // 英文音色
        public static final VoiceInfo BV503 = VoiceInfo.builder()
                .voiceId("BV503_streaming")
                .name("活力女声-Ariana")
                .locale("en-US")
                .gender("Female")
                .shortName("活力女声-Ariana")
                .supportEmotion(false)
                .build();

        public static final VoiceInfo BV504 = VoiceInfo.builder()
                .voiceId("BV504_streaming")
                .name("活力男声-Jackson")
                .locale("en-US")
                .gender("Male")
                .shortName("活力男声-Jackson")
                .supportEmotion(false)
                .build();

        // 日语音色
        public static final VoiceInfo BV524 = VoiceInfo.builder()
                .voiceId("BV524_streaming")
                .name("日语男声")
                .locale("ja-JP")
                .gender("Male")
                .shortName("日语男声")
                .supportEmotion(false)
                .build();

        private PresetVoices() {}
    }
}
