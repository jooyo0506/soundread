package com.soundread.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 大模型统一配置 (系统级兜底 - Level 2)
 *
 * 三级瀑布降级:
 * Level 1: sys_tier_policy.resource_rules (运营策略, 最高优先)
 * Level 2: 本类 (application.yml, 系统基线) ← 你在这里
 * Level 3: TierPolicyDto.ResourceRules 的 Java 默认值 (兜底)
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class LlmProperties {

    /** 默认供应商标识 (doubao / deepseek / qwen / minimax) */
    private String defaultProvider = "doubao";

    /** 默认模型名称或端点 ID */
    private String defaultModel = "doubao-seed-2-0-lite-260215";

    /** 默认 API 基础 URL */
    private String defaultBaseUrl = "https://ark.cn-beijing.volces.com/api/v3";

    /** 创作温度参数 */
    private Double temperature = 0.7;

    /** 默认最大 Token 数 */
    private Integer maxTokens = 4096;

    /**
     * 各供应商的 API Key (敏感信息, 只存 yml, 不进策略表)
     * key = 供应商标识, value = API Key
     */
    private Map<String, String> apiKeys = new HashMap<>();

    /**
     * 各供应商的 Base URL 映射
     * key = 供应商标识, value = API Base URL
     */
    private Map<String, String> baseUrls = new HashMap<>();

    /**
     * 运行时降级供应商链 (有序): 主模型失败后依次尝试
     * 例如: [qwen, zhipu, gemini]
     */
    private java.util.List<String> fallbackProviders = new java.util.ArrayList<>();

    /**
     * 各供应商的默认模型名映射 (降级时使用)
     * key = 供应商标识, value = 模型名
     * 例如: { qwen: deepseek-v3, zhipu: glm-4-flash }
     */
    private Map<String, String> defaultModels = new HashMap<>();
}
