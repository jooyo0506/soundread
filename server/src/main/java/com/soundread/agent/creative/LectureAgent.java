package com.soundread.agent.creative;

import org.springframework.stereotype.Component;

/**
 * 知识讲解创作 Agent
 *
 * <p>
 * 专注：知识科普、逻辑清晰、教学节奏、通俗易懂。
 * 输出适合教学讲解类配音的知识性内容。
 * </p>
 */
@Component
public class LectureAgent extends AbstractCreativeAgent {

    @Override
    public String getTypeCode() {
        return "lecture";
    }

    @Override
    protected String getTypeName() {
        return "知识讲解";
    }

    @Override
    protected String getInspirationStyleGuide() {
        return "生成 6 个适合知识讲解的话题灵感。\n" +
                "要求：有知识增量、解释复杂概念、引发好奇心。\n" +
                "风格参考：历史揭秘、科学原理、心理学效应、经济学入门、哲学思辨、冷知识趣闻。";
    }

    @Override
    protected String getOutlineStructureGuide() {
        return "请为这次知识讲解设计教学结构：\n" +
                "1. 开头用问题或趣事引入主题\n" +
                "2. 正文按逻辑递进展开（是什么 → 为什么 → 怎么做）\n" +
                "3. 穿插案例、类比、故事让知识点生动\n" +
                "4. 结尾做精炼总结，强化记忆点";
    }

    @Override
    protected String getSectionCountGuide() {
        return "sections 数量：3-5 段，每段讲解一个核心知识点。";
    }

    @Override
    protected String getCharacterGuide() {
        return "characters 可为空数组，知识讲解通常无需角色设定。";
    }

    @Override
    protected String getContentRoleEnhancement() {
        return "【知识讲解创作要求】\n" +
                "1. 语言通俗易懂，复杂概念用类比解释\n" +
                "2. 逻辑结构清晰：论点→论据→结论\n" +
                "3. 穿插「你知道吗」「举个例子」等引导语增强互动感\n" +
                "4. 数据和事实要准确，不编造\n" +
                "5. 节奏适中，给听众消化的空间";
    }

    @Override
    protected String getOutputFormatRule() {
        return "直接输出正文，不要输出标题行，不要使用任何标题标记。" +
                "以讲解叙事的方式呈现，全文控制在600字以内。" +
                "逻辑清晰、案例生动，要有完整的开头引入、知识拆解和升华总结。";
    }

    @Override
    public int getMaxOutputTokens() {
        return 1500; // 600字×1.5=900 tokens + 格式缓冲
    }

    @Override
    protected String getRewriteStyleGuide() {
        return "改写时保持知识准确性，优化表达的通俗性和逻辑性。";
    }
}
