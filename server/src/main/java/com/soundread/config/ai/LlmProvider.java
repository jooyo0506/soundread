package com.soundread.config.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * LLM 供应商策略接口 (Strategy Pattern)
 *
 * 面试话术:
 * "我们定义了一个统一的 LlmProvider 策略接口, 每个大模型供应商(豆包/DeepSeek/千问/MiniMax)
 * 各自实现这个接口。业务层只依赖接口编程, 具体用哪个供应商由配置文件的 ai.llm.active 决定,
 * 运行时通过 Spring 容器动态选择, 实现了 开闭原则(OCP) — 新增供应商只需加一个实现类+一段配置,
 * 不用改任何已有代码。"
 */
public interface LlmProvider {

    /**
     * 供应商标识 (与 application.yml 中 ai.llm.providers 的 key 对应)
     */
    String getName();

    /**
     * 获取同步聊天模型 (用于内容审核、情感分析等一次性调用)
     */
    ChatLanguageModel getChatModel();

    /**
     * 获取流式聊天模型 (用于 AI 剧本生成的 SSE 打字机效果)
     */
    StreamingChatLanguageModel getStreamingChatModel();
}
