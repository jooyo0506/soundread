package com.soundread.controller.ttsv2;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 情感剧本标签润色请求
 */
@Data
public class AiEmotionRequest {

    /**
     * 用户输入的原始剧本/台词文本
     */
    @NotBlank(message = "原文本不能为空")
    private String text;
}
