package com.soundread.service;

import com.soundread.agent.drama.ScriptWriterAgent;
import com.soundread.agent.emotion.DirectorScriptAgent;
import com.soundread.agent.emotion.MoodAnalyzerAgent;
import com.soundread.agent.emotion.QuickDubbingAgent;
import com.soundread.config.ai.LlmRouter;
import com.soundread.model.entity.User;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 剧本生成服务 — 双模式 Agent 策略路由
 *
 * 核心架构 (Agent 方向亮点):
 *
 * 1. 声明式 Agent 分离: QuickDubbingAgent 和 DirectorScriptAgent 各自有
 * 精准的 @SystemMessage, 比一个万能 prompt 生成质量更高。
 *
 * 2. 运行时动态构建: 每次请求通过 AiServices.builder() 手动构建 Agent,
 * 底层模型由 LlmRouter 根据用户等级动态路由, 支持运营端秒级切模型。
 *
 * 3. 策略模式路由: 前端传入 mode ("quick"/"director"),
 * 后端选择对应 Agent, 一个接口支持两种创作范式。
 *
 * LLM 调用流程:
 * 前端 → Controller → AiScriptService.generateSceneScript(mode=?)
 * ├─ mode="quick" → QuickDubbingAgent.generate(userPrompt) → LLM
 * └─ mode="director" → DirectorScriptAgent.generate(userPrompt) → LLM
 *
 * 每次请求只走一个 Agent, 不会同时走两种提示词。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiScriptService {

    private final LlmRouter llmRouter;

    /**
     * 流式生成 AI 配音脚本 (SSE 推送到前端)
     *
     * @param user   当前用户 (用于确定该等级走哪个模型)
     * @param prompt 用户输入的灵感描述
     * @return 返回 TokenStream 供调用方挂载回调和 start()
     */
    public TokenStream generateScriptStream(User user, String prompt) {
        // 1. 动态获取当前用户等级对应的流式模型 (三级瀑布降级)
        StreamingChatLanguageModel model = llmRouter.getStreamingModelWithFallback(user);

        // 2. 手动构建 Agent — @SystemMessage 依然生效, 但底层模型是动态的
        ScriptWriterAgent agent = AiServices.builder(ScriptWriterAgent.class)
                .streamingChatLanguageModel(model)
                .build();

        // 3. 返回流式建造者, 不直接 .start() 避免异步生命周期断裂
        return agent.generateScriptStream(prompt);
    }

    /**
     * 向后兼容的重载 (无 User 参数时, 使用默认模型配置)
     */
    public TokenStream generateScriptStream(String prompt) {
        User defaultUser = new User();
        defaultUser.setTierCode("user");
        return generateScriptStream(defaultUser, prompt);
    }

    /**
     * AI智能情感润色：为纯文本自动添加全局 [#语气] 指令
     */
    public String enhanceEmotionTags(User user, String plainText) {
        // 1. 获取对应的模型
        ChatLanguageModel model = llmRouter.getChatModelWithFallback(user);
        // 2. 构造严格的系统提示词
        String systemPrompt = "配音导演。在台词开头添加一个全局语气指令 [#用xxx的语气]。" +
                "规则：1.只加一个开头指令 2.不改原文 3.禁止括号标注如（轻笑）4.只输出处理后的剧本，无解释";

        // 3. 调用大模型
        String raw = model.generate(
                dev.langchain4j.data.message.SystemMessage.from(systemPrompt),
                dev.langchain4j.data.message.UserMessage.from(plainText)).content().text();
        return stripThinkTags(raw);
    }

    /**
     * 双模式 AI 台本生成 — 策略路由核心方法
     *
     * 根据 mode 参数选择不同的声明式 Agent:
     * - "quick" → QuickDubbingAgent: 凭空独立创作, 侧重创意冲击力
     * - "director" → DirectorScriptAgent: 承接前情续写, 侧重剧情连贯性
     *
     * 两个 Agent 各有精准的 @SystemMessage, 比单一万能 prompt 生成质量更高。
     *
     * @param user        当前用户 (决定走哪个模型)
     * @param instruction 语气指令 (如 "用低沉深情的语气")
     * @param wordCount   目标字数
     * @param theme       场景主题 (如 "深夜电台")
     * @param context     前情上文 (仅 director 模式有值)
     * @param mode        创作模式: "quick" 或 "director"
     */
    public String generateSceneScript(User user, String instruction, int wordCount,
            String theme, String context, String mode) {
        // 1. 动态获取当前用户等级对应的同步模型
        ChatLanguageModel model = llmRouter.getChatModelWithFallback(user);

        // 2. 构造 UserMessage (把结构化参数组装成自然语言输入)
        String userPrompt;

        if ("director".equals(mode)) {
            // ── 🎬 剧情导演模式: 包含前情上文 ──
            String contextText = (context != null && !context.trim().isEmpty()) ? context : "暂无前情";
            String themeText = (theme != null && !theme.trim().isEmpty()) ? theme : "自由发挥";

            userPrompt = String.format(
                    "【前情提要】%s\n【语气指令】%s\n【场景主题】%s\n【字数要求】严格控制在 %d 字左右",
                    contextText, instruction, themeText, wordCount);

            log.info("[AiScriptService] 🎬 剧情导演模式 | 指令={}, 字数={}, 主题={}", instruction, wordCount, themeText);

            // 3a. 手动构建 DirectorScriptAgent (动态模型 + 声明式 prompt)
            DirectorScriptAgent agent = AiServices.builder(DirectorScriptAgent.class)
                    .chatLanguageModel(model)
                    .build();

            return stripThinkTags(agent.generate(userPrompt));

        } else {
            // ── ⚡ 快速配音模式 (默认): 无 context, 独立创作 ──
            String themeText = (theme != null && !theme.trim().isEmpty()) ? theme : "随意发挥";

            userPrompt = String.format(
                    "【语气风格】%s\n【风格主题】%s\n【字数要求】严格控制在 %d 字左右",
                    instruction, themeText, wordCount);

            log.info("[AiScriptService] ⚡ 快速配音模式 | 指令={}, 字数={}, 主题={}", instruction, wordCount, themeText);

            // 3b. 手动构建 QuickDubbingAgent (动态模型 + 声明式 prompt)
            QuickDubbingAgent agent = AiServices.builder(QuickDubbingAgent.class)
                    .chatLanguageModel(model)
                    .build();

            return stripThinkTags(agent.generate(userPrompt));
        }
    }

    /**
     * 向后兼容: 旧调用方不传 mode 时, 默认走 quick 模式
     */
    public String generateSceneScript(User user, String instruction, int wordCount,
            String theme, String context) {
        return generateSceneScript(user, instruction, wordCount, theme, context, "quick");
    }

    /**
     * AI 语气推荐 — 分析剧情上文，返回匹配的语气方向
     *
     * <p>
     * 解决 Director Mode 中"剧情上文"和"发音要求"情感方向不一致的 UX 问题。
     * 调用 MoodAnalyzerAgent，输出 2~3 个匹配的语气推荐（JSON 数组格式）。
     * </p>
     *
     * @param user    当前用户 (决定走哪个模型)
     * @param context 剧情上文
     * @return JSON 数组格式的语气推荐列表
     */
    public String analyzeMood(User user, String context) {
        ChatLanguageModel model = llmRouter.getChatModelWithFallback(user);

        log.info("[AiScriptService] 🧠 语气推荐 | 上文={}", context);

        MoodAnalyzerAgent agent = AiServices.builder(MoodAnalyzerAgent.class)
                .chatLanguageModel(model)
                .build();

        return stripThinkTags(agent.analyze(context));
    }

    /**
     * ★ 过滤 LLM 思维链标签 — Qwen 等模型会输出 <think>...</think> 推理过程
     * 这些内容不应展示给用户
     */
    private String stripThinkTags(String text) {
        if (text == null)
            return "";
        // 移除 <think>...</think> 及其内容（支持跨行）
        return text.replaceAll("(?s)<think>.*?</think>", "").trim();
    }
}
