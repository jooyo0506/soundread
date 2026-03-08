package com.soundread.agent.creative;

import org.springframework.stereotype.Component;

/**
 * 广播剧/短剧 创作 Agent — 对话驱动架构
 *
 * <p>
 * 核心理念：一键生成完整对话剧本，不走小说的"大纲→逐章"模式。
 * 支持三种对话模式：双人对话、三人对话、群像对话。
 * 输出严格的角色对白格式，适配 TTS 分角色朗读。
 * </p>
 */
@Component
public class DramaAgent extends AbstractCreativeAgent {

    @Override
    public String getTypeCode() {
        return "drama";
    }

    @Override
    protected String getTypeName() {
        return "短剧";
    }

    @Override
    protected String getInspirationStyleGuide() {
        return "生成 6 个适合AI短剧/广播剧的创作灵感。\n" +
                "要求：有强烈反转、身份反差、打脸名场面、高密度冲突。\n" +
                "热门题材参考：霸总甜宠(装穷追妻)、穿越重生(复仇打脸)、闪婚豪门(隐藏身份)、复仇逆袭(实力碾压)、古装宫斗(后宫权谋)、沙雕搞笑(反套路发疯)。";
    }

    @Override
    protected String getOutlineStructureGuide() {
        return "请为这部短剧设计完整的剧情概要和角色设定。\n" +
                "1. 开篇即冲突/即反转，30秒内抓住观众\n" +
                "2. 角色之间必须有强烈的身份反差或情感对立\n" +
                "3. 对话要有潜台词，不要直白表达";
    }

    @Override
    protected String getSectionCountGuide() {
        return "完整剧本一次生成，不分章节。";
    }

    @Override
    protected String getCharacterGuide() {
        return "characters 必须设定完整的角色表，每人标注：\n" +
                "- name: 角色名\n" +
                "- desc: 性格特征、说话风格、身份背景\n" +
                "- voiceType: 推荐声线类型（冷艳女声/浑厚男声/温柔少女/沙哑中年/活泼少年/沉稳旁白等）\n" +
                "角色之间必须有明确的关系冲突或情感张力。";
    }

    @Override
    protected String getContentRoleEnhancement() {
        return "【短剧创作要求 — 严格对话格式】\n" +
                "1. 以多角色对话为唯一叙事方式\n" +
                "2. 每句对话必须独占一行，格式严格为：角色名：对话内容（用中文全角冒号：）\n" +
                "3. 音效标注必须独占一行，格式为 [音效描述]，如 [雨声]、[脚步声]\n" +
                "4. 绝对禁止任何旁白、叙述文字、心理描写！\n" +
                "5. 绝对禁止括号动作描写如（轻笑）(叹气)！\n" +
                "6. 纯对话推动剧情，通过语气和用词暗示情绪";
    }

    @Override
    protected String getOutputFormatRule() {
        return "直接输出对话剧本正文。\n" +
                "【严格格式要求】\n" +
                "- 对白格式：角色名：对白内容（中文全角冒号：，每句独占一行）\n" +
                "- 音效格式：[音效描述]（独占一行，如 [雨声]）\n" +
                "- 禁止任何旁白和叙述文字\n" +
                "- 禁止使用括号动作描写如（微笑）";
    }

    @Override
    public int getMaxOutputTokens() {
        return 2000;
    }

    @Override
    protected String getTtsAdaptationRule() {
        return "1. 每句对话独立一行，方便 TTS 分角色朗读。" +
                "2. 用标点断句，单句不超过40字。" +
                "3. 可用省略号……制造停顿、破折号——制造语气突转。" +
                "4. 绝对禁止出现圆括号内的动作描写如（轻笑）（叹气）！";
    }

    @Override
    protected String getRewriteStyleGuide() {
        return "改写时保持角色性格一致性和对白的戏剧张力，优化语气节奏。";
    }

    // ======================== 对话模式 Prompt 构建 ========================

    /**
     * 根据对话模式生成完整的 System Prompt
     *
     * @param dialogueMode "duo" / "trio" / "ensemble"
     * @param genre        题材偏好（ceo/rebirth/revenge...）
     * @param genreTips    题材创作提示
     * @param characters   角色 JSON（前端传入）
     * @return 完整 System Prompt
     */
    public String buildDialogueSystemPrompt(String dialogueMode, String genre,
            String genreTips, String characters) {
        StringBuilder sb = new StringBuilder();

        // 1. AI 角色定义
        sb.append("你是顶级AI短剧编剧，专门创作爆款对话剧本。\n");
        sb.append(getContentRoleEnhancement());

        // 2. 对话模式约束
        sb.append("\n\n【对话模式约束】\n");
        switch (dialogueMode) {
            case "duo":
                sb.append("严格双人对话模式：\n");
                sb.append("- 只有2个角色交替对话\n");
                sb.append("- 对话节奏：A→B→A→B，每回合推进一个情节点\n");
                sb.append("- 15~25句对话，400~600字\n");
                sb.append("- 情绪递进：试探→冲突→爆发→反转\n");
                break;
            case "trio":
                sb.append("严格三人对话模式：\n");
                sb.append("- 只有3个角色参与对话\n");
                sb.append("- 三角关系对白：A对B说→C插话→局面反转\n");
                sb.append("- 20~35句对话，500~800字\n");
                sb.append("- 利用第三人制造信息差和戏剧张力\n");
                break;
            case "ensemble":
            default:
                sb.append("群像对话模式（4~6个角色）：\n");
                sb.append("- 多角色场景对话，有主角和配角之分\n");
                sb.append("- 25~40句对话，600~1000字\n");
                sb.append("- 角色之间有阵营/立场区分，互相博弈\n");
                break;
        }

        // 3. 角色设定
        if (characters != null && !characters.isBlank()) {
            sb.append("\n【角色设定】\n").append(characters);
        }

        // 4. 题材风格
        if (genreTips != null && !genreTips.isBlank()) {
            sb.append("\n\n【题材创作要点】\n").append(genreTips);
        }

        // 5. 输出规则
        sb.append("\n\n【输出规则】：");
        sb.append(getOutputFormatRule());

        // 6. TTS 适配
        sb.append("\n\n【TTS适配规则】（极其重要！）：");
        sb.append(getTtsAdaptationRule());

        return sb.toString();
    }

    /**
     * 对话模式对应的字数限制
     */
    public String getDialogueWordLimit(String mode) {
        return switch (mode) {
            case "duo" -> "400-600";
            case "trio" -> "500-800";
            case "ensemble" -> "600-1000";
            default -> "400-600";
        };
    }

    /**
     * 对话模式对应的 maxTokens
     */
    public int getDialogueMaxTokens(String mode) {
        return switch (mode) {
            case "duo" -> 1200;
            case "trio" -> 1600;
            case "ensemble" -> 2000;
            default -> 1200;
        };
    }

    // ======================== 结构化创作参数（保留兼容） ========================

    @Override
    protected String getUnitName() {
        return "幕";
    }

    @Override
    protected String getWordLimit() {
        return "400-600";
    }

    @Override
    protected boolean hasVoiceType() {
        return true;
    }
}
