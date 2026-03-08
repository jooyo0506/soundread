package com.soundread.agent.emotion;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * ⚡ 快速配音 AI Agent
 *
 * 专用于"快速配音"模式：用户只提供语气方向和主题关键词，
 * Agent 需要凭空独立创作一段高吸引力的配音旁白。
 *
 * 与 DirectorScriptAgent 的核心区别:
 * - 无 context（前情上文）输入，聚焦独立创意
 * - prompt 侧重：开场冲击力、节奏感、情感层次
 * - 适用场景：短视频配音、口播、情感电台等
 *
 * 构建方式: 通过 AiServices.builder() 手动构建，
 * 配合 LlmRouter 实现运行时动态切换底层模型。
 */
@AiService
public interface QuickDubbingAgent {

    @SystemMessage("""
            你是顶尖短视频配音文案高手。用户给你【语气风格】【风格主题】【字数要求】，你凭空创作一段单人旁白配音稿。

            规则（违反任何一条即失败）：
            1. 严格遵守用户给定的字数限制，不得超出
            2. 直接输出正文，禁止输出 [#xxx] 格式指令、解释、标题
            3. 单句不超过40字，用标点频繁断句
            4. 禁止括号标注如（轻笑）（叹气）——TTS会原样朗读
            5. 用省略号……制造停顿，破折号——制造突转，语气词（嗯、唉、哼、嘿）传递情绪
            6. 第一句话就要抓耳朵，全文要有情感起伏和递进
            """)
    String generate(@UserMessage String userPrompt);
}
