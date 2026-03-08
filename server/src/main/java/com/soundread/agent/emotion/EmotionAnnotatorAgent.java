package com.soundread.agent.emotion;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 情感标注 Agent（LangChain4j 声明式）⭐ 有声小说核心
 *
 * <p>
 * 负责为小说分段文本做情感标注：
 * 1. 插入 cot 标签（TTS 2.0 局部情感控制）
 * 2. 生成 context_texts（TTS 2.0 全局语气指令）
 * 3. 输出 emotion_label + tension_level
 * 4. 更新 previousSummary（供下一段使用）
 * </p>
 *
 * @author SoundRead
 */
public interface EmotionAnnotatorAgent {

        @SystemMessage("""
                        有声书情感标注导演。为小说段落标注语音情感，输出严格 JSON。

                        任务：
                        1. cot标签：在情感变化句前插入 <cot text=情感描述>句子</cot>（单句≤64字符，只标变化处）
                        2. context_texts：一句话描述整段语气（如"用低沉悲伤的声音缓慢地讲述"）
                        3. emotion_label：2~4字主情绪（悲伤/紧张/温暖/愤怒）
                        4. tension_level：1~10（1=舒缓, 5=正常, 10=极度紧张）
                        5. summary：≤50字段落摘要（传递给下一段）

                        输出格式（严格JSON，无其他文字）：
                        {"annotated_text":"含cot标签的文本","context_texts":"语气指令","emotion_label":"标签","tension_level":5,"summary":"摘要"}
                        """)
        @UserMessage("""
                        章节: {{chapterTitle}}
                        上一段情绪: {{currentMood}}（紧张度: {{tensionLevel}}）
                        上一段摘要: {{previousSummary}}

                        请标注以下段落:
                        {{rawText}}
                        """)
        String annotate(@V("chapterTitle") String chapterTitle,
                        @V("currentMood") String currentMood,
                        @V("tensionLevel") int tensionLevel,
                        @V("previousSummary") String previousSummary,
                        @V("rawText") String rawText);
}
