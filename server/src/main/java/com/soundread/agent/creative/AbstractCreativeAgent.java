package com.soundread.agent.creative;

import com.soundread.model.entity.CreativeTemplate;
import com.soundread.model.entity.StudioProject;
import com.soundread.model.entity.StudioSection;

import java.util.List;

/**
 * 创作 Agent 抽象基类 — 模板方法 + 策略模式
 *
 * <p>
 * 提取 8 个 Agent 的公共逻辑：
 * <ul>
 * <li>大纲上下文注入</li>
 * <li>角色设定注入</li>
 * <li>续写衔接（最近段落末尾内容）</li>
 * <li>TTS 适配输出规则</li>
 * <li>改写 Prompt 的通用模板</li>
 * </ul>
 * 子类只需覆写类型特化的 Prompt 部分。
 * </p>
 *
 * <p>
 * <b>注意</b>：混合记忆（摘要 + RAG）的上下文构建由 StudioService 在调用前完成，
 * 通过 {@code memoryContext} 参数传入，Agent 不直接依赖 SectionMemoryService。
 * </p>
 *
 * @author SoundRead
 */
public abstract class AbstractCreativeAgent implements CreativeAgent {

    // ======================== 灵感生成 ========================

    @Override
    public String buildInspirationPrompt(String typeName) {
        return typeName + "灵感生成器。" + getInspirationStyleGuide() +
                "\n生成6个灵感，每个15-30字，有画面感和冲突感。输出纯JSON数组：[\"灵感1\",...]，无其他文字。";
    }

    /**
     * 子类覆写：灵感风格指南
     * 
     * <p>
     * 例如小说偏戏剧冲突，播客偏社会话题。
     * </p>
     */
    protected abstract String getInspirationStyleGuide();

    // ======================== 大纲生成 ========================

    @Override
    public String buildOutlinePrompt(String typeInfo, String inspiration) {
        return typeInfo + "内容策划师。" + getOutlineStructureGuide() +
                "\n数量要求：" + getSectionCountGuide() + "（不足将被拒绝！）\n" +
                getCharacterGuide() +
                "\n输出纯JSON（无markdown包裹）：" +
                "{\"title\":\"8~20字标题\",\"synopsis\":\"150~250字概要\"," +
                "\"sections\":[{\"title\":\"4~12字\",\"summary\":\"20~50字\"}]," +
                "\"characters\":[{\"name\":\"名\",\"description\":\"描述\",\"voiceType\":\"声线\"}]}";
    }

    /**
     * 结构化大纲生成 — 含风格偏好和目标章/幕数
     *
     * <p>
     * 前端传入 style（题材偏好）和 targetCount（章/幕数），
     * 由 Agent 构建包含 JSON schema 的完整 Prompt。
     * </p>
     */
    @Override
    public String buildStructuredOutlinePrompt(String typeInfo, String inspiration,
            String style, int targetCount) {
        String unitName = getUnitName();
        String charSpec = hasVoiceType()
                ? "\"characters\": [{\"name\": \"角色名\", \"desc\": \"性格/身份描述/说话风格\", \"voiceType\": \"推荐声线\"}]"
                : "\"characters\": [{\"name\": \"角色名\", \"desc\": \"性格/身份描述\"}]";

        return "你是一位" + typeInfo + "策划师。" + getOutlineStructureGuide() +
                "\n\n请严格按照以下 JSON 格式输出（不要输出任何其他内容）：\n" +
                "{\n" +
                "  \"synopsis\": \"200字以内的故事梗概\",\n" +
                "  " + charSpec + ",\n" +
                "  \"chapters\": [\n" +
                "    {\"plot\": \"详细剧情走向(50-100字)\", \"keyEvents\": \"核心事件(20字)\", \"foreshadowing\": \"伏笔/悬念(20字)\"},\n"
                +
                "    ...\n" +
                "  ]\n" +
                "}\n" +
                "注意：\n" +
                "- chapters 数组长度必须为 " + targetCount + "\n" +
                "- 每" + unitName + "的 plot 要详细到可以直接写正文（50-100字）\n" +
                "- keyEvents 写本" + unitName + "最关键的转折/冲突\n" +
                "- foreshadowing 写本" + unitName + "埋下的伏笔\n" +
                "- 不要在JSON值中使用未转义的双引号";
    }

    // ======================== 结构化内容生成 ========================

    /**
     * 构建结构化创作的 User Message — 消除前端 Prompt
     *
     * <p>
     * 前端只传结构化数据（chapterIndex, outlinePlot, userExtra），
     * 后端根据类型自动构建字数限制、格式要求等完整用户消息。
     * </p>
     */
    @Override
    public String buildChapterUserMessage(int chapterIndex, String outlinePlot,
            String keyEvents, String foreshadowing, String userExtra) {
        String unitName = getUnitName();
        String wordLimit = getWordLimit();
        int num = chapterIndex + 1;

        StringBuilder sb = new StringBuilder();

        // 本章/幕细纲
        sb.append("【本").append(unitName).append("细纲 - 第").append(num).append(unitName).append("】\n");
        sb.append("剧情梗概：").append(outlinePlot != null ? outlinePlot : "无").append("\n");
        sb.append("核心事件：").append(keyEvents != null && !keyEvents.isBlank() ? keyEvents : "无").append("\n");
        sb.append("伏笔埋设：").append(foreshadowing != null && !foreshadowing.isBlank() ? foreshadowing : "无");

        // 用户补充
        if (userExtra != null && !userExtra.isBlank()) {
            sb.append("\n\n【用户补充要求】").append(userExtra);
        }

        // 生成指令（字数限制等，全由后端控制！）
        sb.append("\n\n请撰写第").append(num).append(unitName).append("正文。");
        sb.append("【字数硬性限制】").append(wordLimit).append("字！");
        sb.append("直接输出正文，不要输出标题或章节号。");

        return sb.toString();
    }

    /** 子类覆写：章/幕单位名称（默认"章"） */
    protected String getUnitName() {
        return "章";
    }

    /** 子类覆写：字数限制范围（默认"1000-2000"） */
    protected String getWordLimit() {
        return "1000-2000";
    }

    /** 子类覆写：角色是否需要 voiceType 字段（默认 false） */
    protected boolean hasVoiceType() {
        return false;
    }

    /** 子类覆写：大纲结构指南 */
    protected abstract String getOutlineStructureGuide();

    /** 子类覆写：推荐段落/章节数量 */
    protected abstract String getSectionCountGuide();

    /** 子类覆写：角色设定指南 */
    protected String getCharacterGuide() {
        return "如果内容类型无需角色，characters 可为空数组。";
    }

    // ======================== 内容生成 ========================

    @Override
    public String buildContentSystemPrompt(CreativeTemplate template, StudioProject project,
            List<StudioSection> existingSections, String userInput,
            String memoryContext) {
        StringBuilder sb = new StringBuilder();

        // 1. AI 角色定义（来自数据库模板 + Agent 特化补充）
        sb.append(template.getAiRole());
        sb.append("\n\n").append(getContentRoleEnhancement());

        // 2. 大纲上下文（只注入当前章节规划，减少 token 消耗）
        if (project.getOutline() != null && !project.getOutline().isEmpty()) {
            String currentChapterPlan = extractCurrentChapterPlan(project.getOutline(), existingSections.size());
            sb.append("\n\n【当前章节规划】：\n").append(currentChapterPlan);
        }

        // 3. 角色设定
        if (project.getCharacters() != null && !project.getCharacters().isEmpty()) {
            sb.append("\n\n【角色设定】：\n").append(project.getCharacters());
        }

        // 4. 记忆上下文（摘要 + RAG，由 StudioService 构建后传入）
        if (memoryContext != null && !memoryContext.isBlank()) {
            sb.append("\n\n").append(memoryContext);
        }

        // 5. 续写衔接锚点：注入上一段末尾 300 字，让 AI 紧接着写
        if (!existingSections.isEmpty()) {
            StudioSection lastSection = existingSections.get(existingSections.size() - 1);
            String tail = lastSection.getContent();
            if (tail != null && !tail.isBlank()) {
                if (tail.length() > 300)
                    tail = tail.substring(tail.length() - 300);
                sb.append("\n\n【续写衔接 — 上一幕/章结尾】：\n").append(tail);
                sb.append("\n请从这里紧接着展开下一段，保持角色和情节连贯。不要重复上文内容。");
            }
        }

        // 6. 类型特化输出规则
        sb.append("\n\n【输出规则】：");
        sb.append(getOutputFormatRule());

        // 7. TTS 适配规则（所有类型通用）
        sb.append("\n\n【TTS 适配规则】（极其重要！）：");
        sb.append(getTtsAdaptationRule());

        return sb.toString();
    }

    /**
     * 子类覆写：内容生成时的 AI 角色增强描述
     * 
     * <p>
     * 在数据库的 aiRole 基础上补充类型特化的创作指南。
     * </p>
     */
    protected abstract String getContentRoleEnhancement();

    /**
     * 子类覆写：输出格式规则
     * 
     * <p>
     * 例如小说要 300 字正文，新闻要五要素播报体。
     * </p>
     */
    protected abstract String getOutputFormatRule();

    /**
     * TTS 适配规则 — 所有类型通用
     * 
     * <p>
     * 子类如需增强可覆写此方法。
     * </p>
     */
    protected String getTtsAdaptationRule() {
        return "1. 用标点断句，任何两处标点之间的单句不超过40字。" +
                "2. 用省略号……制造停顿、用破折号——制造语气突转、用短句制造力量感。" +
                "3. 可用语气词增加生动感：嗯、唉、哼、嘿、呵、哈、啧。" +
                "4. 绝对禁止出现任何圆括号标注如（轻笑）（吸气）（叹气）（低声）等！TTS会当文字朗读！";
    }

    // ======================== 改写段落 ========================

    @Override
    public String buildRewriteSystemPrompt(String instruction, String originalContent) {
        return "你是一位专业的" + getTypeName() + "文字编辑。\n" +
                getRewriteStyleGuide() + "\n" +
                "用户会给你一段原文和改写指令，请严格按照指令改写。\n" +
                "直接输出改写后的全文，不要输出解释或对比。\n\n" +
                "【原文】：\n" + originalContent + "\n\n" +
                "【改写指令】：" + instruction;
    }

    /**
     * 子类覆写：该类型的名称（用于改写 Prompt）
     */
    protected abstract String getTypeName();

    /**
     * 子类覆写：改写风格指南
     */
    protected String getRewriteStyleGuide() {
        return "改写时保持原文的核心意思和叙事节奏，优化语言表达。";
    }

    // ======================== 工具方法 ========================

    /**
     * 从大纲 JSON 中提取当前章节的规划信息
     *
     * <p>
     * 不注入完整大纲，只提取当前要写的章节 title + summary + 简短 synopsis。
     * 节省约 300-800 tokens，降低首 token 延迟。
     * </p>
     */
    private String extractCurrentChapterPlan(String outlineJson, int existingSectionCount) {
        try {
            var obj = com.alibaba.fastjson2.JSON.parseObject(outlineJson);
            if (obj == null)
                return outlineJson;

            StringBuilder plan = new StringBuilder();

            // 注入简短的故事梗概
            String synopsis = obj.getString("synopsis");
            if (synopsis != null && !synopsis.isBlank()) {
                plan.append("故事梗概：").append(synopsis.length() > 100 ? synopsis.substring(0, 100) + "..." : synopsis);
            }

            // 优先读 chapterOutlines（结构化大纲），回退到 chapters/sections
            var chapterOutlines = obj.getJSONArray("chapterOutlines");
            var chapters = obj.getJSONArray("chapters");
            var sections = obj.getJSONArray("sections");

            // 选择可用的章节数组
            var chapArr = chapterOutlines != null ? chapterOutlines
                    : (chapters != null ? chapters : sections);

            if (chapArr != null && existingSectionCount < chapArr.size()) {
                var current = chapArr.getJSONObject(existingSectionCount);
                if (current != null) {
                    // 兼容 plot（结构化）和 title+summary（旧格式）
                    String plot = current.getString("plot");
                    String title = current.getString("title");
                    String summary = current.getString("summary");

                    String unitName = getUnitName();
                    plan.append("\n当前要写的").append(unitName).append("：");
                    if (plot != null && !plot.isBlank()) {
                        plan.append(plot);
                        String ke = current.getString("keyEvents");
                        if (ke != null && !ke.isBlank())
                            plan.append("\n核心事件：").append(ke);
                        String fs = current.getString("foreshadowing");
                        if (fs != null && !fs.isBlank())
                            plan.append("\n伏笔：").append(fs);
                    } else if (title != null) {
                        plan.append(title).append(" — ").append(summary != null ? summary : "");
                    }

                    // 下一章/幕预告
                    if (existingSectionCount + 1 < chapArr.size()) {
                        var next = chapArr.getJSONObject(existingSectionCount + 1);
                        if (next != null) {
                            String nextPlot = next.getString("plot");
                            String nextTitle = next.getString("title");
                            plan.append("\n下一").append(unitName).append("预告：")
                                    .append(nextPlot != null ? nextPlot.substring(0, Math.min(nextPlot.length(), 50))
                                            : (nextTitle != null ? nextTitle : ""));
                        }
                    }
                }
            }

            return plan.length() > 0 ? plan.toString() : outlineJson;
        } catch (Exception e) {
            // JSON 解析失败时回退到完整大纲
            return outlineJson;
        }
    }
}
