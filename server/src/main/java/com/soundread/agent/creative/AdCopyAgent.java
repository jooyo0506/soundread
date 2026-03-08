package com.soundread.agent.creative;

import org.springframework.stereotype.Component;

/**
 * 带货文案创作 Agent
 *
 * <p>
 * 专注：卖点提炼、转化导向、紧迫感制造、口播话术。
 * 输出适合直播带货和短视频电商配音的文案内容。
 * </p>
 */
@Component
public class AdCopyAgent extends AbstractCreativeAgent {

    @Override
    public String getTypeCode() {
        return "ad";
    }

    @Override
    protected String getTypeName() {
        return "带货文案";
    }

    @Override
    protected String getInspirationStyleGuide() {
        return "生成 6 个适合带货文案的产品/场景灵感。\n" +
                "要求：有消费场景、痛点共鸣、种草欲望。\n" +
                "风格参考：美妆护肤、零食美食、家居好物、数码潮品、健身器材、母婴用品。";
    }

    @Override
    protected String getOutlineStructureGuide() {
        return "请为这篇带货文案设计 AIDA 结构：\n" +
                "1. Attention（注意）：用痛点或场景吸引目光\n" +
                "2. Interest（兴趣）：产品亮点和差异化卖点\n" +
                "3. Desire（欲望）：使用场景、效果证言、限时优惠\n" +
                "4. Action（行动）：明确的行动召唤";
    }

    @Override
    protected String getSectionCountGuide() {
        return "sections 数量：3-5 段，按 AIDA 漏斗模型层层递进。";
    }

    @Override
    protected String getCharacterGuide() {
        return "characters 可为空数组，带货文案通常无需角色设定。如有需要可设定主播人设。";
    }

    @Override
    protected String getContentRoleEnhancement() {
        return "【带货文案创作要求】\n" +
                "1. 开头直击痛点或制造场景代入感\n" +
                "2. 卖点表达用「数字+效果」让人信服\n" +
                "3. 语气热情但不浮夸，像朋友安利好物\n" +
                "4. 制造稀缺感和紧迫感（限量/限时/独家）\n" +
                "5. 结尾有明确的行动号召\n" +
                "6. 适合口播，节奏快、信息密度高";
    }

    @Override
    protected String getOutputFormatRule() {
        return "直接输出正文，不要输出标题行，不要使用任何标题标记。" +
                "以口播话术的方式呈现，全文控制在600字以内。" +
                "信息密度高、行动导向明确，要有完整的转化流程：钩子→卖点→信任→行动号召。";
    }

    @Override
    public int getMaxOutputTokens() {
        return 1500; // 600字×1.5=900 tokens + 格式缓冲
    }

    @Override
    protected String getTtsAdaptationRule() {
        return "1. 用短句制造节奏感和力量感。" +
                "2. 关键卖点后适当停顿让听众消化。" +
                "3. 可用感叹号增强语气：太好用了！真的绝了！" +
                "4. 绝对禁止圆括号内的动作描写！";
    }

    @Override
    protected String getRewriteStyleGuide() {
        return "改写时保持卖货导向和紧迫感，优化口播节奏和转化话术。";
    }
}
