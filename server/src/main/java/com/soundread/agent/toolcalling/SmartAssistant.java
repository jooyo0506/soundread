package com.soundread.agent.toolcalling;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 智能助手 Agent — Tool Calling 声明式接口
 *
 * <h3>核心原理</h3>
 * <p>
 * 这个接口不需要你实现！LangChain4j 通过 AiServices.builder() 动态代理生成实现类。
 * 当调用 chat() 时，LLM 会：
 * </p>
 * <ol>
 * <li>读取 @SystemMessage 理解自己的角色</li>
 * <li>读取用户输入</li>
 * <li>查看所有注册的 @Tool 工具列表</li>
 * <li>自主决定需要调用哪些工具（可以是 0 个、1 个或多个）</li>
 * <li>调用工具 → 获取结果 → 综合所有结果生成最终回答</li>
 * </ol>
 *
 * <h3>ChatMemory 对话记忆</h3>
 * <p>
 * 通过 @MemoryId 参数实现多用户记忆隔离。
 * 每个用户拥有独立的 ChatMemory（最近 20 条消息），
 * Caffeine 缓存管理生命周期（30分钟过期 + LRU 淘汰）。
 * </p>
 */
public interface SmartAssistant {

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
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
