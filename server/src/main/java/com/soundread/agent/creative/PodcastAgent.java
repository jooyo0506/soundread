package com.soundread.agent.creative;

import org.springframework.stereotype.Component;

/**
 * AI 播客创作 Agent
 *
 * <p>
 * 专注：对话体叙事、话题讨论、口语化表达、嘉宾交互感。
 * 输出适合播客节目的对谈式内容。
 * </p>
 */
@Component
public class PodcastAgent extends AbstractCreativeAgent {

    @Override
    public String getTypeCode() {
        return "podcast";
    }

    @Override
    protected String getTypeName() {
        return "AI播客";
    }

    @Override
    protected String getInspirationStyleGuide() {
        return "生成 6 个适合播客节目的话题灵感。\n" +
                "要求：有讨论性、观点碰撞空间、贴近听众兴趣。\n" +
                "风格参考：科技前沿、职场成长、社会热点、文化观察、生活方式、心理健康。";
    }

    @Override
    protected String getOutlineStructureGuide() {
        return "请为这期播客设计节目段落结构：\n" +
                "1. 开头引入话题（制造好奇心）\n" +
                "2. 正文分 3-5 个讨论角度展开\n" +
                "3. 每个角度有核心观点 + 案例/故事支撑\n" +
                "4. 结尾做总结或抛出开放性问题";
    }

    @Override
    protected String getSectionCountGuide() {
        return "sections 数量：5-8 段，每段围绕一个子话题展开讨论。";
    }

    @Override
    protected String getCharacterGuide() {
        return "characters 设定主播和嘉宾（1-2人），标注各自的专业背景和说话风格。";
    }

    @Override
    protected String getContentRoleEnhancement() {
        return "【播客创作要求】\n" +
                "1. 使用口语化、对谈式的语言风格\n" +
                "2. 可设计主播+嘉宾的对话节奏：提问→回答→追问→总结\n" +
                "3. 融入个人故事、案例和数据让观点更有说服力\n" +
                "4. 语气亲切自然，像朋友聊天而非正式演讲\n" +
                "5. 适当使用过渡语：「说到这个」「对了」「其实吧」增强口语感";
    }

    @Override
    protected String getOutputFormatRule() {
        return "第一行输出本段话题标题，格式为 ##标题名称（4~10个字）。" +
                "第二行起输出正文，以对谈或独白形式呈现。" +
                "正文控制在300字以内，口语自然、节奏轻快。";
    }

    @Override
    protected String getRewriteStyleGuide() {
        return "改写时保持口语化风格和对谈节奏，让内容听起来更自然流畅。";
    }
}
