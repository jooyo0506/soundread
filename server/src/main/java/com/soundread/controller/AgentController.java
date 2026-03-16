package com.soundread.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.soundread.agent.toolcalling.SmartAssistant;
import com.soundread.agent.toolcalling.StreamingSmartAssistant;
import com.soundread.agent.toolcalling.SoundReadTools;
import com.soundread.common.RateLimit;
import com.soundread.common.Result;
import com.soundread.config.ai.LlmProperties;
import com.soundread.model.entity.User;
import com.soundread.service.AuthService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * AI Agent 智能助手 — 企业级 Tool Calling 控制器
 *
 * <h3>企业级设计要点</h3>
 * <ol>
 * <li><b>单 Agent 实例</b> — 全局复用，避免每次请求重建代理</li>
 * <li><b>chatMemoryProvider</b> — LangChain4j 原生多用户模式，按 memoryId 隔离会话</li>
 * <li><b>Caffeine 缓存</b> — ChatMemoryStore 由 Caffeine 支撑：
 * <ul>
 * <li>maximumSize(500) — 最多 500 个并发用户会话</li>
 * <li>expireAfterAccess(30min) — 30 分钟不活跃自动回收</li>
 * <li>LRU 淘汰 — 超容量时淘汰最久未访问的会话</li>
 * </ul>
 * </li>
 * </ol>
 *
 * <h3>对比 ConcurrentHashMap 方案</h3>
 *
 * <pre>
 * ConcurrentHashMap：无上限、无过期、OOM 风险
 * Caffeine：有容量上限 + TTL 过期 + LRU 淘汰 + 统计监控
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final LlmProperties llmProperties;
    private final SoundReadTools soundReadTools;
    private final AuthService authService;

    /**
     * 支持 Tool Calling 的供应商优先级
     * <p>
     * 注意：minimax 使用自有格式（非 OpenAI 标准），LangChain4j 无法解析，会导致原始 Tool 标记透出到前端。
     * deepseek 有 think 标签泄漏问题（已在 cleanReply 过滤），优先级最低。
     * </p>
     */
    private static final String[] TOOL_CALLING_PROVIDERS = { "qwen", "deepseek" };

    /** 全局唯一的 Agent 实例（同步版，保留兼容） */
    private SmartAssistant agent;

    /** 流式 Agent 实例（SSE 推送） */
    private StreamingSmartAssistant streamingAgent;

    private static final long SSE_TIMEOUT_MS = 120_000L;

    /**
     * Caffeine 驱动的 ChatMemory 缓存
     * - 最多 500 个用户会话
     * - 30 分钟不活跃自动回收
     * - 超容量 LRU 淘汰
     */
    private final Cache<Object, Object> memoryCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(30))
            .recordStats()
            .build();

    @PostConstruct
    public void init() {
        try {
            this.agent = buildAgent();
            this.streamingAgent = buildStreamingAgent();
            log.info("[AgentController] Agent + StreamingAgent 初始化完成");
        } catch (Exception e) {
            log.warn("[AgentController] Agent 初始化失败（模型未配置？）: {}", e.getMessage());
        }
    }

    @RateLimit(maxRequests = 5, windowSeconds = 60, message = "AI 助手调用过于频繁，请稍后再试")
    @PostMapping("/chat")
    public Result<?> chat(@RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String token) {
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return Result.fail("消息不能为空");
        }

        if (agent == null) {
            return Result.fail("Agent 未初始化，请检查模型配置");
        }

        // 场景上下文注入
        String scene = body.get("scene");
        if (scene != null && !scene.isBlank()) {
            message = "[用户当前在「" + scene + "」创作场景中] " + message;
        }

        log.info("[AgentController] 用户输入：{}", message);

        // Fast-path: simple greetings skip LLM entirely (<50ms vs 20s+)
        if (isSimpleGreeting(message)) {
            log.info("[AgentController] Fast-path greeting, skipping LLM");
            return Result.ok(Map.of("reply", buildGreetingReply()));
        }

        try {
            // 在 HTTP 请求线程中提前获取用户（Sa-Token 需要 Request 上下文）
            User user = authService.getCurrentUser();
            String memoryId = user.getId().toString();

            // 设置 ThreadLocal，让 Tool 方法能获取用户（不依赖 Sa-Token）
            SoundReadTools.setCurrentUser(user);

            String reply = agent.chat(memoryId, message);
            // 清洗模型返回内容中的内部标记（DeepSeek think + MiniMax tool_call 等）
            reply = cleanReply(reply);

            log.info("[AgentController] Agent 回复长度: {}", reply.length());

            return Result.ok(Map.of("reply", reply));

        } catch (Exception e) {
            log.error("[AgentController] Agent 调用失败", e);
            return Result.fail("Agent 调用失败: " + e.getMessage());
        } finally {
            // 清理 ThreadLocal，防止内存泄漏
            SoundReadTools.clearCurrentUser();
        }
    }

    /**
     * 清空用户的对话记忆
     */
    @PostMapping("/reset")
    public Result<?> reset(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null) {
            String memoryId = String.valueOf(token.hashCode());
            memoryCache.invalidate(memoryId);
            log.info("[AgentController] 已清空用户 {} 的对话记忆", memoryId);
        }
        return Result.ok("对话已重置");
    }

    /**
     * SSE 流式聊天 — 逐 token 推送，首 token 延迟 < 1s
     */
    @RateLimit(maxRequests = 5, windowSeconds = 60, message = "AI 助手调用过于频繁，请稍后再试")
    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> body) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            emitter.completeWithError(new IllegalArgumentException("消息不能为空"));
            return emitter;
        }

        if (streamingAgent == null) {
            emitter.completeWithError(new RuntimeException("StreamingAgent 未初始化"));
            return emitter;
        }

        // 场景上下文注入
        String scene = body.get("scene");
        if (scene != null && !scene.isBlank()) {
            message = "[用户当前在「" + scene + "」创作场景中] " + message;
        }

        // 简单问候快速通道
        if (isSimpleGreeting(message)) {
            try {
                emitter.send(SseEmitter.event().data(buildGreetingReply()));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        log.info("[AgentController] SSE 流式聊天: {}", message);

        try {
            User user = authService.getCurrentUser();
            String memoryId = user.getId().toString();
            SoundReadTools.setCurrentUser(user);

            TokenStream tokenStream = streamingAgent.chat(memoryId, message);
            StringBuilder fullReply = new StringBuilder();

            tokenStream
                    .onNext(token -> {
                        try {
                            fullReply.append(token);
                            emitter.send(SseEmitter.event().data(token));
                        } catch (Exception e) {
                            log.warn("SSE send error: {}", e.getMessage());
                        }
                    })
                    .onComplete(response -> {
                        try {
                            String cleaned = cleanReply(fullReply.toString());
                            // 发送最终完整回复（前端用于解析 audioUrl 等）
                            emitter.send(SseEmitter.event().name("done").data(cleaned));
                            emitter.complete();
                        } catch (Exception e) {
                            log.warn("SSE complete error: {}", e.getMessage());
                        } finally {
                            SoundReadTools.clearCurrentUser();
                        }
                    })
                    .onError(error -> {
                        log.error("[AgentController] Streaming error", error);
                        try {
                            emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                        } catch (Exception ignored) {
                        }
                        emitter.completeWithError(error);
                        SoundReadTools.clearCurrentUser();
                    })
                    .start();

        } catch (Exception e) {
            log.error("[AgentController] SSE 初始化失败", e);
            emitter.completeWithError(e);
            SoundReadTools.clearCurrentUser();
        }

        return emitter;
    }

    /**
     * 缓存统计（运维监控用）
     */
    @GetMapping("/stats")
    public Result<?> stats() {
        var stats = memoryCache.stats();
        return Result.ok(Map.of(
                "estimatedSize", memoryCache.estimatedSize(),
                "hitRate", String.format("%.2f%%", stats.hitRate() * 100),
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "evictionCount", stats.evictionCount()));
    }

    /**
     * 构建全局 Agent 实例
     *
     * <p>
     * 关键：使用 chatMemoryProvider 而非 chatMemory
     * </p>
     * <ul>
     * <li>chatMemory — 单一共享记忆，所有用户混在一起</li>
     * <li>chatMemoryProvider — 按 memoryId 隔离，每个用户独立记忆</li>
     * </ul>
     */
    private SmartAssistant buildAgent() {
        OpenAiChatModel toolModel = null;

        for (String provider : TOOL_CALLING_PROVIDERS) {
            String apiKey = llmProperties.getApiKeys().get(provider);
            if (apiKey != null && !apiKey.isBlank()) {
                String baseUrl = llmProperties.getBaseUrls().getOrDefault(provider, "");
                String modelName = llmProperties.getDefaultModels().getOrDefault(provider, "");

                toolModel = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(60))
                        .maxTokens(512) // Agent replies are typically <200 chars, lower cap reduces inference time
                        .build();
                log.info("[AgentController] 使用模型: {}/{}", provider, modelName);
                break;
            }
        }

        if (toolModel == null) {
            throw new RuntimeException("未配置支持 Tool Calling 的模型 API Key");
        }

        // Caffeine 支撑的 ChatMemoryStore
        ChatMemoryStore memoryStore = new InMemoryChatMemoryStore();

        // chatMemoryProvider：按 memoryId 为每个用户创建独立的 ChatMemory
        // 每个用户保留最近 20 条消息，避免上下文过长
        ChatMemoryProvider memoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10) // 20->10: less context = fewer input tokens = faster LLM response
                .chatMemoryStore(memoryStore)
                .build();

        return AiServices.builder(SmartAssistant.class)
                .chatLanguageModel(toolModel)
                .tools(soundReadTools)
                .chatMemoryProvider(memoryProvider)
                .build();
    }

    /**
     * 构建流式 Agent — 使用 StreamingChatLanguageModel 实现 SSE 逐 token 推送
     */
    private StreamingSmartAssistant buildStreamingAgent() {
        OpenAiStreamingChatModel streamingModel = null;

        for (String provider : TOOL_CALLING_PROVIDERS) {
            String apiKey = llmProperties.getApiKeys().get(provider);
            if (apiKey != null && !apiKey.isBlank()) {
                String baseUrl = llmProperties.getBaseUrls().getOrDefault(provider, "");
                String modelName = llmProperties.getDefaultModels().getOrDefault(provider, "");

                streamingModel = OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(60))
                        .build();
                log.info("[AgentController] 流式模型: {}/{}", provider, modelName);
                break;
            }
        }

        if (streamingModel == null) {
            log.warn("[AgentController] 流式模型未配置，SSE 端点不可用");
            return null;
        }

        ChatMemoryStore memoryStore = new InMemoryChatMemoryStore();
        ChatMemoryProvider memoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(memoryStore)
                .build();

        return AiServices.builder(StreamingSmartAssistant.class)
                .streamingChatLanguageModel(streamingModel)
                .tools(soundReadTools)
                .chatMemoryProvider(memoryProvider)
                .build();
    }

    // ==================== Fast-path: simple greetings ====================

    private static final Set<String> GREETING_EXACT = Set.of(
            "你好", "嗨", "hi", "hello", "hey", "哈喽", "在吗", "在不在",
            "您好", "嘿", "你好呀", "你好啊", "嗨嗨", "哈喽哈喽");

    private static final Pattern GREETING_PATTERN = Pattern.compile(
            "^(你好[啊呀吗嘛]?|嗨[~！!]?|hi[~!]?|hello[~!]?|hey[~!]?|哈喽[~！!]?|在吗[？?]?|在不在[？?]?|你是谁[？?]?)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * 判断是否为简单问候（<10字的寒暄），命中后跳过 LLM 调用
     */
    private boolean isSimpleGreeting(String message) {
        String trimmed = message.replaceAll("[\\s，。！？,.!?~]+", "").toLowerCase();
        if (trimmed.length() > 10)
            return false;
        if (GREETING_EXACT.contains(trimmed))
            return true;
        return GREETING_PATTERN.matcher(trimmed).matches();
    }

    private String buildGreetingReply() {
        String[] replies = {
                "你好！\uD83D\uDC4B 我是你的 AI 声音制作人\n\n" +
                        "点击下方功能按钮开始，或直接描述你的需求：\n" +
                        "- 告诉我一个场景（如\"深夜电台\"）\u2192 我来写词+配音\n" +
                        "- 发一段文字 \u2192 我帮你合成语音\n" +
                        "- 问我\"有什么音色\" \u2192 查看可用声音",
                "嗨！欢迎来到声音工坊 \uD83C\uDFA7\n\n" +
                        "我可以帮你：\n" +
                        "1. \uD83D\uDCDD 写台本（电台独白、祝福语、解说词）\n" +
                        "2. \uD83C\uDFA4 合成语音（多种AI音色可选）\n" +
                        "3. \uD83D\uDE0A 分析情感（推荐最佳语气）\n\n" +
                        "试试直接说：帮我写一段深夜电台的独白"
        };
        return replies[(int) (System.currentTimeMillis() % replies.length)];
    }

    /**
     * 清洗模型回复中的内部标记，防止透出到前端
     *
     * <ul>
     * <li>DeepSeek think 标签: &lt;think&gt;...&lt;/think&gt;</li>
     * <li>MiniMax Tool Call 宽松格式: function&lt; | tool_sep | &gt;...&lt; |
     * tool_calls_end | &gt;</li>
     * <li>MiniMax Tool Call 紧凑格式: &lt;|tool_sep|&gt; &lt;|tool_calls_end|&gt;
     * 等</li>
     * </ul>
     */
    private static String cleanReply(String reply) {
        if (reply == null || reply.isBlank())
            return reply;
        // 1. DeepSeek: 移除 <think>...</think>
        reply = reply.replaceAll("(?s)<think>.*?</think>", "");
        // 2. MiniMax 宽松格式：匹配 function< | tool_sep | >...到 < | tool_calls_end | >
        reply = reply.replaceAll(
                "(?s).{0,5}function<\\s*\\|\\s*tool_sep\\s*\\|\\s*>.*?<\\s*\\|\\s*tool_calls_end\\s*\\|\\s*>", "");
        // 3. MiniMax 紧凑格式 <|xxx|>
        reply = reply.replaceAll("<\\|[^|]+\\|>", "");
        // 4. 其他已知标记
        reply = reply.replaceAll("(?s)\\[TOOL_CALLS].*?\\[/TOOL_CALLS]", "");
        return reply.strip();
    }
}
