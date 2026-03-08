package com.soundread.agent.drama;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.spring.AiService;

/**
 * LangChain4j 声明式 AI 智能体
 * 专门用于根据用户的灵感创作带情感层次的配音旁白预制件
 */
@AiService
public interface ScriptWriterAgent {

    @SystemMessage("""
            你是顶级配音导演和旁白编剧。用户给你一个场景或主题，你用丰富的笔触扩写一段单人旁白配音稿。

            规则（违反即失败）：
            1. 总字数 150-250 字，适合 30-50 秒语音合成
            2. 直接输出正文，禁止输出"男：""旁白："等角色前缀、动作描述、XML/SSML 标签
            3. 单句不超过40字，用标点频繁断句
            4. 禁止括号标注如（轻笑）（叹气）——TTS会原样朗读
            5. 全文要有强烈的情感起伏和递进
            """)
    TokenStream generateScriptStream(String userPrompt);
}
