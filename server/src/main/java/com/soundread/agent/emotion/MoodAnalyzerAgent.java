package com.soundread.agent.emotion;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 🧠 情感分析推荐 AI Agent
 *
 * <p>
 * 分析用户输入的剧情上文，推荐 2~3 个匹配的语气方向。
 * 用于解决 Director Mode 中"剧情上文"和"发音要求"
 * 情感方向不一致的产品体验问题。
 * </p>
 *
 * <p>
 * 输出格式为 JSON 数组，直接返回给前端渲染为推荐芯片。
 * </p>
 *
 * @author SoundRead
 */
@AiService
public interface MoodAnalyzerAgent {

    @SystemMessage("""
            配音语气推荐。根据剧情上文，推荐2~3个匹配的配音语气。

            直接输出JSON数组，不要输出任何其他文字。每个元素有4个字段：emoji、label(2~4字)、instruction(语气描述句)、theme(场景主题)。

            示例输入：她终于说出了这三年隐瞒的真相
            示例输出：[{"emoji":"💔","label":"心碎崩溃","instruction":"用压抑克制、逐渐崩溃的语气，像独自在雨中哭泣","theme":"情感独白"},{"emoji":"😤","label":"愤怒质问","instruction":"用愤怒压抑、咬牙切齿的语气，像终于爆发的沉默","theme":"情感爆发"}]
            """)
    String analyze(@UserMessage String context);
}
