package com.soundread.config.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 同步模型运行时降级代理 — 主模型失败时自动切换备用供应商
 *
 * <p>
 * 比流式版本简单：同步调用直接 try-catch 重试即可。
 * </p>
 */
@Slf4j
public class FallbackChatModel implements ChatLanguageModel {

    private final ChatLanguageModel primary;
    private final String primaryName;
    private final List<String> fallbackProviders;
    private final LlmRouter router;

    public FallbackChatModel(ChatLanguageModel primary,
            String primaryName,
            List<String> fallbackProviders,
            LlmRouter router) {
        this.primary = primary;
        this.primaryName = primaryName;
        this.fallbackProviders = fallbackProviders;
        this.router = router;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        // 1. 先尝试主模型
        try {
            return primary.generate(messages);
        } catch (Exception e) {
            log.warn("[Fallback] 同步主模型 [{}] 失败: {}", primaryName, e.getMessage());
        }

        // 2. 依次尝试 fallback 链
        for (int i = 0; i < fallbackProviders.size(); i++) {
            String provider = fallbackProviders.get(i);
            try {
                log.info("[Fallback] ★ 同步模型切换到备用供应商 [{}] (第 {} 个)", provider, i + 1);
                ChatLanguageModel fallbackModel = router.buildChatModelForProvider(provider);
                return fallbackModel.generate(messages);
            } catch (Exception e) {
                log.warn("[Fallback] 备用供应商 [{}] 也失败: {}", provider, e.getMessage());
            }
        }

        throw new RuntimeException("所有 AI 供应商均不可用");
    }
}
