package com.soundread.agent.creative;

import com.soundread.model.entity.CreativeTemplate;
import com.soundread.model.entity.StudioProject;
import com.soundread.model.entity.StudioSection;

import java.util.List;

/**
 * 创作 Agent 策略接口 — 每种创作类型一个独立 Agent
 *
 * <p>
 * 遵循策略模式，将 8 种创作类型的 AI Prompt 逻辑解耦为独立策略类：
 * <ul>
 * <li>有声小说 (novel) → {@code NovelAgent}</li>
 * <li>广播剧 (drama) → {@code DramaAgent}</li>
 * <li>AI播客 (podcast) → {@code PodcastAgent}</li>
 * <li>情感电台 (radio) → {@code RadioAgent}</li>
 * <li>知识讲解 (lecture) → {@code LectureAgent}</li>
 * <li>带货文案 (ad) → {@code AdCopyAgent}</li>
 * <li>有声绘本 (picture_book) → {@code PictureBookAgent}</li>
 * <li>新闻播报 (news) → {@code NewsAgent}</li>
 * </ul>
 * </p>
 *
 * <p>
 * 新增创作类型只需实现此接口并加 @Component 注解，无需修改 StudioService。
 * </p>
 *
 * @author SoundRead
 */
public interface CreativeAgent {

    /**
     * 该 Agent 支持的 typeCode
     * 
     * @return novel / drama / podcast / radio / lecture / ad / picture_book / news
     */
    String getTypeCode();

    /**
     * 构建灵感种子生成的 System Prompt
     * 
     * <p>
     * 每种类型有不同的灵感风格：小说偏戏剧冲突，播客偏话题性，新闻偏时事性。
     * </p>
     */
    String buildInspirationPrompt(String typeName);

    /**
     * 构建大纲生成的 System Prompt
     * 
     * <p>
     * 小说需要章节、播客需要段落、新闻需要五要素。
     * </p>
     */
    String buildOutlinePrompt(String typeInfo, String inspiration);

    /**
     * 构建结构化大纲生成的完整 Prompt（含风格偏好和目标章/幕数）
     *
     * <p>
     * 用于小说/广播剧的结构化创作流程。前端只传 style + count，
     * 由 Agent 构建完整的 JSON schema Prompt。
     * </p>
     *
     * @param typeInfo    类型名称（小说/广播剧）
     * @param inspiration 用户灵感
     * @param style       题材风格偏好（如 palace/heroine 等），可为 null
     * @param targetCount 目标章/幕数量
     */
    default String buildStructuredOutlinePrompt(String typeInfo, String inspiration,
            String style, int targetCount) {
        // 默认回退到原有大纲 Prompt
        return buildOutlinePrompt(typeInfo, inspiration);
    }

    /**
     * 构建结构化内容生成时的 User Message
     *
     * <p>
     * 前端只传 chapterIndex + outlinePlot + userExtra，
     * 由 Agent 构建包含字数限制、格式要求的完整用户消息。
     * 消除前后端 Prompt 冲突。
     * </p>
     *
     * @param chapterIndex  当前章/幕索引（0-based）
     * @param outlinePlot   本章/幕的细纲内容
     * @param keyEvents     核心事件
     * @param foreshadowing 伏笔
     * @param userExtra     用户额外补充（可为 null）
     * @return 完整的用户消息
     */
    default String buildChapterUserMessage(int chapterIndex, String outlinePlot,
            String keyEvents, String foreshadowing, String userExtra) {
        // 默认实现：简单拼接
        return "请撰写第" + (chapterIndex + 1) + "章正文。\n" + outlinePlot;
    }

    /**
     * 构建内容生成的 System Prompt
     * 
     * <p>
     * 核心方法，注入：AI 角色定义 + 大纲上下文 + 记忆上下文 + 类型特化输出规则。
     * 上下文注入（摘要记忆、RAG、续写衔接）由 AbstractCreativeAgent 基类统一处理。
     * </p>
     */
    String buildContentSystemPrompt(CreativeTemplate template, StudioProject project,
            List<StudioSection> existingSections, String userInput,
            String memoryContext);

    /**
     * 构建改写段落的 System Prompt
     * 
     * @param instruction     改写指令（改写/扩写/缩写）
     * @param originalContent 原始段落内容
     */
    String buildRewriteSystemPrompt(String instruction, String originalContent);

    /**
     * 该类型内容生成时的最大输出 token 数
     *
     * <p>
     * 用于在 LLM API 层硬切输出长度，防止 Prompt 约束被模型忽略。
     * 1 中文字 ≈ 1.5 token，1500 字 ≈ 2200 tokens。
     * </p>
     *
     * @return 最大输出 token 数（默认 2048）
     */
    default int getMaxOutputTokens() {
        return 2048;
    }
}
