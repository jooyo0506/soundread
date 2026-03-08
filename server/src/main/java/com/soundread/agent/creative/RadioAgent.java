package com.soundread.agent.creative;

import org.springframework.stereotype.Component;

/**
 * 情感电台创作 Agent
 *
 * <p>
 * 专注：深夜情感、治愈系独白、走心语录、情绪疗愈。
 * 输出适合夜间情感电台的温柔叙事内容。
 * </p>
 */
@Component
public class RadioAgent extends AbstractCreativeAgent {

    @Override
    public String getTypeCode() {
        return "radio";
    }

    @Override
    protected String getTypeName() {
        return "情感电台";
    }

    @Override
    protected String getInspirationStyleGuide() {
        return "生成 6 个适合情感电台的创作灵感。\n" +
                "要求：触动人心、有共鸣感、适合深夜倾听。\n" +
                "风格参考：异地恋思念、失恋治愈、亲情感悟、自我成长、孤独陪伴、岁月回忆。";
    }

    @Override
    protected String getOutlineStructureGuide() {
        return "请为这期情感电台设计叙事脉络：\n" +
                "1. 以情绪为线索串联内容\n" +
                "2. 从引入情境 → 展开故事 → 情感升华 → 温暖收尾\n" +
                "3. 每段有独立的情感色彩但共同服务于主题";
    }

    @Override
    protected String getSectionCountGuide() {
        return "sections 数量：3-5 段，每段对应一个情感阶段。";
    }

    @Override
    protected String getCharacterGuide() {
        return "characters 可为空数组，情感电台通常为单人独白叙事。如有需要可设定一个主播人设。";
    }

    @Override
    protected String getContentRoleEnhancement() {
        return "【情感电台创作要求】\n" +
                "1. 使用温柔、细腻、内省的语言风格\n" +
                "2. 以第一人称或第二人称叙事（对听众说话）\n" +
                "3. 融入生活场景和五感描写，制造画面感\n" +
                "4. 情感真挚不矫情，共鸣点要精准\n" +
                "5. 句子节奏舒缓，适合深夜低声朗读\n" +
                "6. 可适当引用诗句或歌词增加文学感";
    }

    @Override
    protected String getOutputFormatRule() {
        return "直接输出正文，不要输出标题行，不要使用任何标题标记。" +
                "以独白或对听众倾诉的方式呈现，全文控制在600字以内。" +
                "语句轻柔、留白有韵味，宁短勿长。要有完整的情感弧线和温暖的收束。";
    }

    @Override
    public int getMaxOutputTokens() {
        return 1500; // 600字×1.5=900 tokens + 格式缓冲
    }

    @Override
    protected String getTtsAdaptationRule() {
        return "1. 句子宜短不宜长，制造朗读时的呼吸感和停顿美。" +
                "2. 大量使用省略号……制造思绪绵延的感觉。" +
                "3. 单句不超过30字，适合深夜低语式朗读。" +
                "4. 绝对禁止圆括号内的动作描写！";
    }

    @Override
    protected String getRewriteStyleGuide() {
        return "改写时保持温柔治愈的语调，增强情感共鸣力和画面感。";
    }
}
