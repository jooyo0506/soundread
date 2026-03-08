package com.soundread.agent.creative;

import org.springframework.stereotype.Component;

/**
 * 有声绘本创作 Agent
 *
 * <p>
 * 专注：儿童风格、画面描述、分镜节奏、温馨叙事。
 * 输出适合儿童有声绘本配音的亲子内容。
 * </p>
 */
@Component
public class PictureBookAgent extends AbstractCreativeAgent {

    @Override
    public String getTypeCode() {
        return "picture_book";
    }

    @Override
    protected String getTypeName() {
        return "有声绘本";
    }

    @Override
    protected String getInspirationStyleGuide() {
        return "生成 6 个适合儿童有声绘本的创作灵感。\n" +
                "要求：有教育意义、画面感强、适合3-8岁儿童。\n" +
                "风格参考：动物冒险、友谊故事、勇气成长、自然探索、传统文化、情绪管理。";
    }

    @Override
    protected String getOutlineStructureGuide() {
        return "请为这本有声绘本设计画面式叙事结构：\n" +
                "1. 每一页/每一段对应一个画面场景\n" +
                "2. 情节简单明了，适合儿童理解\n" +
                "3. 有明确的教育主题或品格培养目标\n" +
                "4. 结尾温馨正面，给孩子安全感";
    }

    @Override
    protected String getSectionCountGuide() {
        return "sections 数量：5-8 页，每页对应一个简短的画面场景。";
    }

    @Override
    protected String getCharacterGuide() {
        return "characters 设定可爱的角色形象（动物/孩子），标注性格特征和推荐声线（活泼童声/温柔女声）。";
    }

    @Override
    protected String getContentRoleEnhancement() {
        return "【有声绘本创作要求】\n" +
                "1. 语言简洁明快，用词适合儿童理解\n" +
                "2. 每段描述一个画面场景，有色彩和动作描写\n" +
                "3. 可以有简单的对话和拟声词（汪汪！哗啦啦！）\n" +
                "4. 节奏轻快温馨，适合睡前朗读\n" +
                "5. 传递正面价值观：勇气、友爱、分享、好奇心";
    }

    @Override
    protected String getOutputFormatRule() {
        return "直接输出故事正文，不要输出标题行。" +
                "根据适龄范围控制字数（2-4岁约300字，4-6岁约500字，6-8岁约600字）。" +
                "句子短小精悍，适合儿童听。故事要有完整的开头、发展和温暖的结尾。";
    }

    @Override
    public int getMaxOutputTokens() {
        return 1500; // 600-800字×1.5≈900-1200 tokens + 格式缓冲
    }

    @Override
    protected String getTtsAdaptationRule() {
        return "1. 句子要短！每句不超过20字，适合慢速温柔朗读。" +
                "2. 可用拟声词增加趣味：咕噜噜、蹦蹦跳、哗啦啦。" +
                "3. 语气词多用：哇、呀、呢、啦，增加童趣感。" +
                "4. 绝对禁止圆括号动作标注！";
    }

    @Override
    protected String getRewriteStyleGuide() {
        return "改写时保持儿童友好的语言风格，确保内容积极向上、画面感丰富。";
    }
}
