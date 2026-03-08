package com.soundread.config.ai;

import com.soundread.model.dto.TierPolicyDto;
import com.soundread.model.entity.User;
import com.soundread.service.TierPolicyService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM 动态模型路由器 — 三级瀑布式降级 + 运行时自动故障转移
 *
 * <pre>
 * 配置降级 (选模型):
 *   Level 1: sys_tier_policy.resource_rules → 运营后台热更新 (最高优先)
 *   Level 2: LlmProperties (application.yml) → 系统基线
 *   Level 3: ResourceRules Java 默认值 → 兜底防空指针
 *
 * 运行时降级 (模型挂了):
 *   主模型 onError → fallback-providers 链依次重试 → 用户无感知
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmRouter {

    private final TierPolicyService tierPolicyService;
    private final LlmProperties llmProperties;

    /** 模型实例缓存: key = "provider:model:baseUrl" */
    private final Map<String, StreamingChatLanguageModel> streamingCache = new ConcurrentHashMap<>();
    private final Map<String, ChatLanguageModel> chatCache = new ConcurrentHashMap<>();

    /**
     * 为指定用户动态获取流式聊天模型 (AI 剧本生成用)
     */
    public StreamingChatLanguageModel getStreamingModel(User user) {
        return getStreamingModel(user, null);
    }

    /**
     * 带 maxTokens 限制的流式模型
     */
    public StreamingChatLanguageModel getStreamingModel(User user, Integer maxTokens) {
        ResolvedConfig config = resolveConfig(user);
        String cacheKey = config.cacheKey() + (maxTokens != null ? ":max" + maxTokens : "");

        return streamingCache.computeIfAbsent(cacheKey, k -> {
            log.info("[LlmRouter] 创建流式模型: tier={}, model={}, maxTokens={}",
                    user.getTierCode(), config.model, maxTokens);
            return buildStreamingModel(config, maxTokens);
        });
    }

    /**
     * ★ 带运行时降级的流式模型 — 主模型失败后自动切换备用供应商
     *
     * <p>
     * 对调用方完全透明，用户无感知。推荐所有面向用户的功能使用此方法。
     * </p>
     */
    public StreamingChatLanguageModel getStreamingModelWithFallback(User user) {
        return getStreamingModelWithFallback(user, null);
    }

    /**
     * ★ 带 maxTokens 限制和运行时降级的流式模型
     */
    public StreamingChatLanguageModel getStreamingModelWithFallback(User user, Integer maxTokens) {
        ResolvedConfig primaryConfig = resolveConfig(user);
        StreamingChatLanguageModel primaryModel = getStreamingModel(user, maxTokens);

        List<String> fallbacks = llmProperties.getFallbackProviders();
        if (fallbacks == null || fallbacks.isEmpty()) {
            return primaryModel; // 没配降级链，直接返回主模型
        }

        // 过滤掉与主模型相同的供应商
        List<String> effectiveFallbacks = fallbacks.stream()
                .filter(p -> !p.equals(primaryConfig.provider))
                .toList();

        if (effectiveFallbacks.isEmpty()) {
            return primaryModel;
        }

        return new FallbackStreamingChatModel(
                primaryModel,
                primaryConfig.provider,
                index -> {
                    String provider = effectiveFallbacks.get(index);
                    ResolvedConfig fallbackConfig = resolveProviderConfig(provider);
                    log.info("[LlmRouter] 创建降级流式模型: provider={}, model={}",
                            fallbackConfig.provider, fallbackConfig.model);
                    return buildStreamingModel(fallbackConfig, maxTokens);
                },
                effectiveFallbacks);
    }

    /**
     * 为指定用户动态获取同步聊天模型
     */
    public ChatLanguageModel getChatModel(User user) {
        ResolvedConfig config = resolveConfig(user);

        return chatCache.computeIfAbsent(config.cacheKey(), k -> {
            log.info("[LlmRouter] 创建同步模型: tier={}, provider={}, model={}",
                    user.getTierCode(), config.provider, config.model);

            return OpenAiChatModel.builder()
                    .apiKey(config.apiKey)
                    .modelName(config.model)
                    .baseUrl(config.baseUrl)
                    .temperature(llmProperties.getTemperature())
                    .timeout(Duration.ofSeconds(120))
                    .build();
        });
    }

    /**
     * ★ 带运行时降级的同步模型 — 主模型超时/失败后自动切换备用供应商
     *
     * <p>
     * 同步调用比流式简单：直接 try-catch 重试即可。
     * </p>
     */
    public ChatLanguageModel getChatModelWithFallback(User user) {
        ResolvedConfig primaryConfig = resolveConfig(user);
        ChatLanguageModel primaryModel = getChatModel(user);

        List<String> fallbacks = llmProperties.getFallbackProviders();
        if (fallbacks == null || fallbacks.isEmpty()) {
            return primaryModel;
        }

        List<String> effectiveFallbacks = fallbacks.stream()
                .filter(p -> !p.equals(primaryConfig.provider))
                .toList();

        if (effectiveFallbacks.isEmpty()) {
            return primaryModel;
        }

        // 返回一个代理 ChatLanguageModel，在 generate 时自动降级
        return new FallbackChatModel(primaryModel, primaryConfig.provider, effectiveFallbacks, this);
    }

    /**
     * 运营刷新策略后, 清空模型缓存
     */
    public void invalidateCache() {
        streamingCache.clear();
        chatCache.clear();
        log.info("[LlmRouter] 模型缓存已清空 (运营策略刷新触发)");
    }

    // ========================
    // 内部辅助方法
    // ========================

    /**
     * 为指定供应商构建同步模型 (FallbackChatModel 调用)
     */
    ChatLanguageModel buildChatModelForProvider(String provider) {
        ResolvedConfig config = resolveProviderConfig(provider);
        log.info("[LlmRouter] 创建降级同步模型: provider={}, model={}", config.provider, config.model);
        return OpenAiChatModel.builder()
                .apiKey(config.apiKey)
                .modelName(config.model)
                .baseUrl(config.baseUrl)
                .temperature(llmProperties.getTemperature())
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    /**
     * 构建 OpenAI 兼容的流式模型实例
     */
    private StreamingChatLanguageModel buildStreamingModel(ResolvedConfig config, Integer maxTokens) {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey)
                .modelName(config.model)
                .baseUrl(config.baseUrl)
                .temperature(llmProperties.getTemperature())
                .timeout(Duration.ofSeconds(120));
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }
        return builder.build();
    }

    /**
     * 三级瀑布降级核心逻辑 — 为用户解析最终模型配置
     */
    private ResolvedConfig resolveConfig(User user) {
        String tierCode = user.getTierCode() != null ? user.getTierCode() : "user";

        // Level 1: 运营策略 (最高优先)
        TierPolicyDto.ResourceRules rules = tierPolicyService.getResourceRules(tierCode);
        String provider = rules.getLlmProvider();
        String model = rules.getLlmModel();
        String baseUrl = rules.getLlmBaseUrl();

        // Level 2: application.yml 默认值 (降级)
        if (isBlank(model)) {
            provider = llmProperties.getDefaultProvider();
            model = llmProperties.getDefaultModel();
            baseUrl = llmProperties.getDefaultBaseUrl();
            log.debug("[LlmRouter] 等级 [{}] 未配置模型, 降级到 yml 默认: {}", tierCode, model);
        }

        // 根据 provider 查找对应的 baseUrl
        if (isBlank(baseUrl) || baseUrl.equals(llmProperties.getDefaultBaseUrl())) {
            String providerUrl = llmProperties.getBaseUrls().get(provider);
            if (!isBlank(providerUrl)) {
                baseUrl = providerUrl;
            }
        }

        // API Key: 始终从 yml 的 apiKeys 字典中取
        String apiKey = llmProperties.getApiKeys().getOrDefault(provider, "");
        if (isBlank(apiKey)) {
            apiKey = llmProperties.getApiKeys().values().stream()
                    .filter(v -> !isBlank(v))
                    .findFirst()
                    .orElse("");
            log.warn("[LlmRouter] 供应商 [{}] 未配置 API Key, 使用兜底 Key", provider);
        }

        return new ResolvedConfig(provider, model, baseUrl, apiKey);
    }

    /**
     * 为指定供应商解析配置 (fallback 用)
     */
    private ResolvedConfig resolveProviderConfig(String provider) {
        String apiKey = llmProperties.getApiKeys().getOrDefault(provider, "");
        String baseUrl = llmProperties.getBaseUrls().getOrDefault(provider, "");

        // 从配置文件读取供应商默认模型名，没配则用主模型的 default-model 兜底
        String model = llmProperties.getDefaultModels().getOrDefault(provider, llmProperties.getDefaultModel());

        if (isBlank(baseUrl)) {
            baseUrl = llmProperties.getDefaultBaseUrl();
        }
        if (isBlank(apiKey)) {
            apiKey = llmProperties.getApiKeys().values().stream()
                    .filter(v -> !isBlank(v))
                    .findFirst()
                    .orElse("");
        }

        return new ResolvedConfig(provider, model, baseUrl, apiKey);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 解析后的最终配置 (不可变) */
    private record ResolvedConfig(String provider, String model, String baseUrl, String apiKey) {
        String cacheKey() {
            return provider + ":" + model + ":" + baseUrl;
        }
    }
}
