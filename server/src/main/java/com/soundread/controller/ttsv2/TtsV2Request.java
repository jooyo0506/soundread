package com.soundread.controller.ttsv2;

import lombok.Data;

/**
 * TTS v2.0 合成请求
 */
@Data
public class TtsV2Request {
    /**
     * 合成文本(支持 [#语气指令])
     */
    private String text;

    /**
     * 上文内容(可选)
     */
    private String contextText;

    /**
     * 音色ID
     */
    private String voiceType;

    /**
     * 用户标识
     */
    private String userKey;

    /**
     * 模式: default / voice_command / context
     */
    private String mode = "default";
}
