package com.soundread.controller.ttsv2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS v2.0 合成响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TtsV2Response {
    /**
     * 合成后的对象存储最终音频链接
     */
    private String audioUrl;
}
