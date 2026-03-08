package com.soundread.model;

import lombok.Data;

/**
 * 有声小说情感状态机（Structured Memory）
 *
 * <p>
 * 在 Pipeline 逐段处理时传递和更新，替代 RAG / ConversationMemory。
 * 每处理完一个 segment，EmotionAnnotatorAgent 会更新此状态，
 * 再传给下一个 segment 的标注，实现情感连贯性。
 * </p>
 *
 * <p>
 * <b>为什么不用 RAG？</b>
 * </p>
 * <ul>
 * <li>小说文本本身就是输入，不需要"检索外部知识"</li>
 * <li>previousSummary 做摘要压缩，每段只传 ≤100 字给 LLM，Token 开销极小</li>
 * <li>TTS 的 section_id 由引擎侧维护语音连续性</li>
 * </ul>
 *
 * @author SoundRead
 */
@Data
public class EmotionState {

    /** 当前主情绪（如 "悲伤"、"紧张"、"平静"） */
    private String currentMood;

    /** 紧张度 1~10（用于语速控制：2=舒缓, 5=正常, 8=紧张） */
    private int tensionLevel;

    /** 前一段的摘要（≤100 字，喂给 LLM 做上下文衔接） */
    private String previousSummary;

    /** 上一段 TTS 合成后的 session_id（用于 TTS 2.0 section_id 串联） */
    private String lastSectionId;

    /** 当前章节标题（帮助 LLM 理解大背景） */
    private String chapterTitle;

    /**
     * 创建初始状态（无前文）
     *
     * @param chapterTitle 章节标题
     * @return 初始 EmotionState
     */
    public static EmotionState initial(String chapterTitle) {
        EmotionState state = new EmotionState();
        state.setCurrentMood("平静");
        state.setTensionLevel(5);
        state.setPreviousSummary("");
        state.setLastSectionId(null);
        state.setChapterTitle(chapterTitle);
        return state;
    }

    /**
     * 从上一章末尾状态创建新章开头状态
     *
     * @param prevChapterSummary 上一章的情感总结
     * @param newChapterTitle    新章节标题
     * @return 衔接状态
     */
    public static EmotionState fromPreviousChapter(String prevChapterSummary, String newChapterTitle) {
        EmotionState state = new EmotionState();
        state.setCurrentMood("平静");
        state.setTensionLevel(5);
        state.setPreviousSummary(prevChapterSummary != null ? prevChapterSummary : "");
        state.setLastSectionId(null); // 新章节重建 TTS session
        state.setChapterTitle(newChapterTitle);
        return state;
    }
}
