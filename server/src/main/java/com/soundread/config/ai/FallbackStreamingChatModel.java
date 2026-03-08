package com.soundread.config.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.IntFunction;

/**
 * 运行时降级装饰器 — 主模型失败时自动切换备用供应商
 *
 * <p>
 * 对调用方完全透明，用户无感知。
 * </p>
 *
 * <pre>
 * 主模型 (doubao) → 失败 → 备选1 (qwen) → 失败 → 备选2 (zhipu) → ...
 * </pre>
 */
@Slf4j
public class FallbackStreamingChatModel implements StreamingChatLanguageModel {

    private final StreamingChatLanguageModel primary;
    private final IntFunction<StreamingChatLanguageModel> fallbackFactory;
    private final int fallbackCount;
    private final String primaryName;
    private final List<String> fallbackNames;

    /**
     * @param primary         主模型
     * @param primaryName     主供应商名 (日志用)
     * @param fallbackFactory 按索引创建备用模型 (懒加载)
     * @param fallbackNames   备用供应商名列表 (日志用)
     */
    public FallbackStreamingChatModel(
            StreamingChatLanguageModel primary,
            String primaryName,
            IntFunction<StreamingChatLanguageModel> fallbackFactory,
            List<String> fallbackNames) {
        this.primary = primary;
        this.primaryName = primaryName;
        this.fallbackFactory = fallbackFactory;
        this.fallbackNames = fallbackNames;
        this.fallbackCount = fallbackNames.size();
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        // 先尝试主模型
        primary.generate(messages, new StreamingResponseHandler<AiMessage>() {
            private boolean hasSentTokens = false;

            @Override
            public void onNext(String token) {
                hasSentTokens = true;
                handler.onNext(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                handler.onComplete(response);
            }

            @Override
            public void onError(Throwable error) {
                // 如果已经开始发送 token 了，就不能切换了 (部分内容已送达)
                if (hasSentTokens || fallbackCount == 0) {
                    log.warn("[Fallback] 主模型 [{}] 失败且已开始流式输出, 无法降级: {}",
                            primaryName, error.getMessage());
                    handler.onError(error);
                    return;
                }

                log.warn("[Fallback] 主模型 [{}] 失败, 尝试降级到备用链: {}",
                        primaryName, error.getMessage());
                tryFallback(messages, handler, 0);
            }
        });
    }

    /**
     * 递归尝试 fallback 链中的下一个供应商
     */
    private void tryFallback(List<ChatMessage> messages,
            StreamingResponseHandler<AiMessage> handler,
            int index) {
        if (index >= fallbackCount) {
            log.error("[Fallback] 所有备用供应商均已失败");
            handler.onError(new RuntimeException("所有 AI 供应商均不可用"));
            return;
        }

        String fallbackName = fallbackNames.get(index);
        log.info("[Fallback] ★ 切换到备用供应商 [{}] (第 {} 个)", fallbackName, index + 1);

        try {
            StreamingChatLanguageModel fallbackModel = fallbackFactory.apply(index);
            fallbackModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                private boolean hasSentTokens = false;

                @Override
                public void onNext(String token) {
                    hasSentTokens = true;
                    handler.onNext(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    log.info("[Fallback] ★ 备用供应商 [{}] 成功完成", fallbackName);
                    handler.onComplete(response);
                }

                @Override
                public void onError(Throwable error) {
                    if (hasSentTokens) {
                        handler.onError(error);
                        return;
                    }
                    log.warn("[Fallback] 备用供应商 [{}] 也失败: {}", fallbackName, error.getMessage());
                    tryFallback(messages, handler, index + 1);
                }
            });
        } catch (Exception e) {
            log.warn("[Fallback] 创建备用模型 [{}] 失败: {}", fallbackName, e.getMessage());
            tryFallback(messages, handler, index + 1);
        }
    }
}
