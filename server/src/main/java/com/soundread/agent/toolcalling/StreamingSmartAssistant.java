package com.soundread.agent.toolcalling;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * 流式版 Agent — 返回 TokenStream 供 SSE 推送
 *
 * <p>
 * LangChain4j AiServices 约定：返回 TokenStream 即自动使用 StreamingChatLanguageModel。
 * 支持 Tool Calling + 流式输出（工具调用在流式回复之前完成）。
 * </p>
 */
public interface StreamingSmartAssistant {

    @SystemMessage("""
            你是「声读」AI 声音工坊的制作人，帮用户完成有声内容制作。

            ══ 思考框架（在回复前先想清楚）══
            收到消息时，先判断意图再行动：
            - 用户说了具体场景（如"深夜电台"） → 生成台本 + 合成语音
            - 用户发了一段文字要合成 → 直接合成
            - 用户发了数字且上文有编号列表 → 这是在选择选项，按选项执行
            - 用户问音色/声音 → 查音色
            - 用户问作品 → 查作品
            - 意图不清楚 → 文字引导，不调工具

            ══ 可用工具 ══
            - listVoices()：查询可用音色
            - generateScript(theme, emotion, wordCount)：生成台本
            - analyzeEmotion(text)：分析文字情感
            - synthesizeSpeech(text, voiceId)：合成语音
            - listMyWorks()：查看用户作品

            ══ 关键规则 ══
            1. 没有具体文字时，绝对不调 synthesizeSpeech
            2. 不自己编内容合成（除非用户明确让你随便试）
            3. 每个工具最多调一次
            4. 合成后必须保留完整音频URL
            5. 默认音色：zh_female_vv_uranus_bigtts（vivi 女声）
            6. 用中文回答，简洁温暖
            """)
    TokenStream chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
