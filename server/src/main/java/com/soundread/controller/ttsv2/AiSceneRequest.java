package com.soundread.controller.ttsv2;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 台本生成请求参数
 *
 * <p>
 * 用于 POST /api/tts/v2/generate-scene 接口。
 * 前端根据当前创作模式传入不同的参数组合：
 * <ul>
 * <li>mode="quick" — 快速配音（instruction + theme，无 context）</li>
 * <li>mode="director" — 剧情导演（instruction + theme + context）</li>
 * </ul>
 * 后端根据 mode 选择不同的 AI Agent 和 System Prompt。
 * </p>
 *
 * @author SoundRead
 */
@Data
public class AiSceneRequest {

    /**
     * 场景主题
     *
     * <p>
     * 如：激情带货、悬疑解说、深夜电台
     * </p>
     */
    private String theme;

    /**
     * 语气指令（必填）
     *
     * <p>
     * 如："用低沉深情、节奏舒缓的语气，像深夜电台主播一样"
     * </p>
     */
    @NotBlank(message = "配音指令不能为空")
    private String instruction;

    /**
     * 目标生成字数
     *
     * <p>
     * 范围 10~500，默认 30
     * </p>
     */
    @Min(value = 10, message = "字数不能少于10")
    @Max(value = 500, message = "字数不能超过500")
    private Integer wordCount = 30;

    /**
     * 前情上文（可选，仅 director 模式使用）
     *
     * <p>
     * 如："发现被最亲近的人欺骗后，主角独自走在雨中"
     * </p>
     */
    private String context;

    /**
     * 创作模式
     *
     * <ul>
     * <li>"quick" — ⚡快速配音（默认，走 QuickDubbingAgent）</li>
     * <li>"director" — 🎬剧情导演（走 DirectorScriptAgent）</li>
     * </ul>
     *
     * <p>
     * 后端根据此字段选择不同的 AI Agent 和 System Prompt。
     * </p>
     */
    private String mode = "quick";
}
