package com.soundread.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 等级策略 JSON 映射 DTO
 * <p>
 * 三大控制面：功能开关 / 配额限制 / 资源分配规则
 * </p>
 */
public class TierPolicyDto {

    // ============================
    // 1. 功能开关 (能不能用)
    // ============================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeatureFlags {
        /** 基础 TTS */
        @JsonProperty("tts_basic")
        private boolean ttsBasic = true;

        /** 情感合成 v2 */
        @JsonProperty("tts_emotion_v2")
        private boolean ttsEmotionV2 = false;

        /** AI 播客 */
        @JsonProperty("ai_podcast")
        private boolean aiPodcast = false;

        /** 声音复刻 */
        @JsonProperty("voice_clone")
        private boolean voiceClone = false;

        /** AI 编排 (创作台 AI 写稿) */
        @JsonProperty("ai_script")
        private boolean aiScript = false;

        /** 多语言合成 */
        @JsonProperty("multi_language")
        private boolean multiLanguage = false;

        /** AI 有声小说 */
        @JsonProperty("ai_novel")
        private boolean aiNovel = false;

        /** VIP 发布免审核 */
        @JsonProperty("auto_publish")
        private boolean autoPublish = false;
    }

    // ============================
    // 2. 配额限制 (能用多少)
    // ============================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuotaLimits {
        /** TTS 1.0 基础合成每日字数上限, -1表示无限 */
        @JsonProperty("tts_daily_chars")
        private int ttsDailyChars = 500;

        /** TTS 2.0 情感合成每日字数上限, -1表示无限 */
        @JsonProperty("tts_v2_daily_chars")
        private int ttsV2DailyChars = 0;

        /** 声音克隆总次数上限 */
        @JsonProperty("clone_total_count")
        private int cloneTotalCount = 0;

        /** AI 对话每日次数 */
        @JsonProperty("ask_daily_count")
        private int askDailyCount = 3;

        /** 播客每日生成次数 */
        @JsonProperty("podcast_daily_count")
        private int podcastDailyCount = 0;

        /** AI 剧本编排每日次数 */
        @JsonProperty("ai_script_daily_count")
        private int aiScriptDailyCount = 3;

        /** 云存储空间上限 (MB) */
        @JsonProperty("storage_max_mb")
        private int storageMaxMb = 50;

        /** 作品最大保存数量 */
        @JsonProperty("max_projects")
        private int maxProjects = 5;

        /** 文件保留天数, -1为永久 */
        @JsonProperty("data_retention_days")
        private int dataRetentionDays = 7;

        /** 有声小说每日处理字数上限`(-1无限) */
        @JsonProperty("novel_daily_chars")
        private int novelDailyChars = 0;

        /** 有声小说项目数量上限 */
        @JsonProperty("novel_max_projects")
        private int novelMaxProjects = 0;
    }

    // ============================
    // 3. 资源分配规则 (底层资源倾斜)
    // ============================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceRules {
        /** AI 供应商标识: doubao / deepseek / qwen / minimax */
        @JsonProperty("llm_provider")
        private String llmProvider = "doubao";

        /** 使用的大模型版本或端点 ID */
        @JsonProperty("llm_model")
        private String llmModel = "doubao-seed-2-0-lite-260215";

        /** 大模型 API 基础 URL (运营可按等级路由到不同入口) */
        @JsonProperty("llm_base_url")
        private String llmBaseUrl = "https://ark.cn-beijing.volces.com/api/v3";

        /** 异步任务排队优先级 (0=低, 99=VIP插队) */
        @JsonProperty("task_priority")
        private int taskPriority = 0;

        /** 每秒并发限制 (QPS) */
        @JsonProperty("qps_limit")
        private int qpsLimit = 2;

        /** TTS 音色库可用范围: basic / premium / all */
        @JsonProperty("voice_tier")
        private String voiceTier = "basic";
    }

    // ============================
    // 聚合体：前端登录后一次性下发
    // ============================
    @Data
    public static class UserPolicy {
        private String tierCode;
        private String tierName;
        private FeatureFlags featureFlags;
        private QuotaLimits quotaLimits;
        private ResourceRules resourceRules;
    }
}
