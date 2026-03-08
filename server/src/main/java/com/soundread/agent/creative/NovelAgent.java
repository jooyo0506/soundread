package com.soundread.agent.creative;

import org.springframework.stereotype.Component;

/**
 * AI小说创作 Agent
 *
 * <p>
 * 专注：热门网文题材创作、章节衔接、人物弧光、戏剧冲突、爽点节奏。
 * 输出适合网络阅读的小说正文，段落精练、节奏鲜明、钩子密集。
 * </p>
 */
@Component
public class NovelAgent extends AbstractCreativeAgent {

    @Override
    public String getTypeCode() {
        return "novel";
    }

    @Override
    protected String getTypeName() {
        return "AI小说创作";
    }

    @Override
    protected String getInspirationStyleGuide() {
        return "生成 6 个适合网络小说的创作灵感。\n" +
                "要求：有强烈的戏剧冲突、人物命运转折、悬念钩子。\n" +
                "题材方向参考当前热门趋势：无CP大女主（事业/复仇/成长）、人性博弈/智斗（烧脑反转）、" +
                "脑洞叠加/系统流（弹幕/穿书/心声）、修仙2.0（职场化修仙）、发疯文学（荒诞反内耗）、" +
                "考据式穿越（历史严谨）、年代高干（怀旧逆袭）。";
    }

    @Override
    protected String getOutlineStructureGuide() {
        return "请为这部网络小说设计完整的故事架构：\n" +
                "1. 必须包含起承转合的完整叙事弧线（开篇铺垫→矛盾升级→高潮爆发→结局收束）\n" +
                "2. 每章结尾必须有悬念钩子（Cliffhanger），让读者想继续看下一章\n" +
                "3. 明确主角成长线和核心矛盾，有清晰的人物弧光\n" +
                "4. 角色关系网要立体（正派/反派/灰色角色）\n" +
                "5. 章节之间要有因果递进，不能是孤立的短篇拼凑\n" +
                "6. 爽点分布要均匀，前3章要有核心看点抓住读者";
    }

    @Override
    protected String getSectionCountGuide() {
        return "sections 数量：推荐 5-20 章的完整故事规划。\n" +
                "注意：这只是大纲规划，实际内容会逐章生成，不会一次写完所有章节。\n" +
                "每章的 summary 要包含：场景设定、关键事件、人物情感变化、悬念钩子。";
    }

    @Override
    protected String getCharacterGuide() {
        return "characters 必须设定：主角、对手、关键配角，每人需明确性格特征和行为动机。";
    }

    @Override
    protected String getContentRoleEnhancement() {
        return "【网络小说创作要求】\n" +
                "1. 根据题材选择合适的人称叙事，代入感要强\n" +
                "2. 对话精炼有力，用动作和语气暗示说话人\n" +
                "3. 注重五感描写（视觉/听觉/触觉/嗅觉/味觉）\n" +
                "4. 每段结尾留悬念或反转，推动读者继续阅读\n" +
                "5. 爽点密集，信息差暴击和认知反转要到位\n" +
                "6. 节奏紧凑，避免大段冗余描写";
    }

    @Override
    protected String getOutputFormatRule() {
        return "第一行输出本章标题，格式为 ##标题名称（4~10个字，概括本章核心冲突）。" +
                "第二行起输出正文。" +
                "【字数硬性限制】正文严格控制在 1000-2000 字之间！绝对不允许超过 2000 字！" +
                "要求场景描写精练、情感饱满、节奏紧凑。" +
                "每次只生成一章内容！不要跨章节输出！" +
                "不要输出序号或其他格式前缀。";
    }

    @Override
    protected String getRewriteStyleGuide() {
        return "改写时保持小说的叙事张力和人物一致性，增强画面感和情感渲染力，严格保持在1500字以内。";
    }

    @Override
    public int getMaxOutputTokens() {
        return 2200; // 硬截断：2200 tokens → 最多约2000中文字
    }
}
