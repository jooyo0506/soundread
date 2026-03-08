package com.soundread.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能语气匹配结果
 *
 * <p>
 * 返回给前端的结构化语气推荐，包含主推荐、置信度、匹配来源和备选方案。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartToneResult {

    /** 推荐的语气指令 (如 "压低声音、带点气声、语速放缓") */
    private String instruction;

    /** 匹配到的场景名 (如 "悬疑惊悚"，来自 ai_prompt_role.name) */
    private String sceneName;

    /** 匹配置信度 0-1 */
    private double confidence;

    /** 匹配来源: "rag+llm" 或 "llm" */
    private String source;

    /** 备选语气方案 */
    private List<Alternative> alternatives;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alternative {
        private String instruction;
        private String sceneName;
        private double confidence;
    }
}
