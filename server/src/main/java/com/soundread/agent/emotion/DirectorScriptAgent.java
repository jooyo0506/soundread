package com.soundread.agent.emotion;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 🎬 剧情导演 AI Agent
 *
 * 专用于"剧情导演"模式：用户提供前情上文 + 语气指令，
 * Agent 需要承接剧情脉络，写出逻辑连贯、情感自然衔接的续写台词。
 *
 * 与 QuickDubbingAgent 的核心区别:
 * - 必须依赖 context（前情上文），强调剧情连贯性
 * - prompt 侧重：上下文承接、角色情感延续、逻辑合理性
 * - 适用场景：有声小说、影视解说、多段连续配音等
 *
 * 构建方式: 通过 AiServices.builder() 手动构建，
 * 配合 LlmRouter 实现运行时动态切换底层模型。
 */
@AiService
public interface DirectorScriptAgent {

    @SystemMessage("""
            你是金牌配音导演和剧本续写专家。用户提供【前情提要】【语气指令】【场景主题】【字数要求】，你的任务是承接前情，写出情感自然衔接、逻辑连贯的单人台词。

            规则（违反任何一条即失败）：
            1. 严格遵守用户给定的字数限制，不得超出
            2. 台词必须是"上一幕之后角色自然会说的话"，不能跳脱前情
            3. 直接输出正文，禁止输出 [#xxx] 格式指令、解释、标题
            4. 单句不超过40字，用标点频繁断句
            5. 禁止括号标注如（轻笑）（叹气）——TTS会原样朗读
            6. 用省略号……制造停顿，破折号——制造突转，语气词（嗯、唉、哼、嘿）传递情绪
            """)
    String generate(@UserMessage String userPrompt);
}
