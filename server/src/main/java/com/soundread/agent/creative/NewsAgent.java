package com.soundread.agent.creative;

import org.springframework.stereotype.Component;

/**
 * 新闻播报创作 Agent
 *
 * <p>
 * 专注：新闻五要素、客观表述、标准播报体、信息密度。
 * 输出适合新闻播报配音的正式内容。
 * </p>
 */
@Component
public class NewsAgent extends AbstractCreativeAgent {

    @Override
    public String getTypeCode() {
        return "news";
    }

    @Override
    protected String getTypeName() {
        return "新闻播报";
    }

    @Override
    protected String getInspirationStyleGuide() {
        return "生成 6 个适合新闻播报的选题灵感。\n" +
                "要求：有新闻价值、信息量大、具备时效性。\n" +
                "风格参考：科技突破、社会民生、国际局势、财经动态、文体快讯、环保议题。";
    }

    @Override
    protected String getOutlineStructureGuide() {
        return "请为这篇新闻稿设计倒金字塔结构：\n" +
                "1. 导语（最重要的信息，回答5W1H）\n" +
                "2. 核心事实（关键细节和数据）\n" +
                "3. 背景补充（上下文和原因分析）\n" +
                "4. 后续影响（展望和评论）";
    }

    @Override
    protected String getSectionCountGuide() {
        return "sections 数量：3-4 段，按倒金字塔结构从最重要到最次要排列。";
    }

    @Override
    protected String getCharacterGuide() {
        return "characters 为空数组，新闻播报无需角色设定。";
    }

    @Override
    protected String getContentRoleEnhancement() {
        return "【新闻播报创作要求】\n" +
                "1. 语言客观、准确、简洁，不带个人情感色彩\n" +
                "2. 使用倒金字塔结构，重要信息前置\n" +
                "3. 数据和事实清晰，避免模糊表述\n" +
                "4. 使用新闻用语：据悉、近日、日前、相关人士表示\n" +
                "5. 适合播音腔朗读，节奏稳健庄重";
    }

    @Override
    protected String getOutputFormatRule() {
        return "直接输出新闻正文，不要输出标题行，不要使用任何标题标记。" +
                "以标准播报体呈现，全文控制在400-600字。" +
                "信息密度高、事实准确，导语精炼，要有完整的报道结构。";
    }

    @Override
    public int getMaxOutputTokens() {
        return 1500; // 600字×1.5=900 tokens + 格式缓冲
    }

    @Override
    protected String getTtsAdaptationRule() {
        return "1. 句式严整，单句不超过35字，适合播音腔匀速朗读。" +
                "2. 避免过长的从句，保持语法简洁。" +
                "3. 数字和专有名词后适当停顿。" +
                "4. 绝对禁止圆括号内的描写或注释！";
    }

    @Override
    protected String getRewriteStyleGuide() {
        return "改写时保持新闻的客观性和准确性，优化播报语感和信息结构。";
    }
}
