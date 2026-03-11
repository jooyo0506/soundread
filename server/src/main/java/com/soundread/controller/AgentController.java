package com.soundread.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.soundread.agent.toolcalling.SmartAssistant;
import com.soundread.agent.toolcalling.SoundReadTools;
import com.soundread.common.Result;
import com.soundread.config.ai.LlmProperties;
import com.soundread.model.entity.User;
import com.soundread.service.AuthService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

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
     * 注意：minmax 使用自容格式（非 OpenAI 标准），LangChain4j 无法解析，会导致原始 Tool 标记透出到前端。
     * deepseek 有 think 标签泄漏问题（已在 cleanReply 过滤），优先级最低。
     * </p>
     */
    private static final String[] TOOL_CALLING_PROVIDERS = { "qwen", "deepseek" };

    /** 全局唯一的 Agent 实例 */
    private SmartAssistant agent;

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
            log.info("[AgentController] ✅ Agent 初始化完成");
        } catch (Exception e) {
            log.warn("[AgentController] ⚠️ Agent 初始化失败（模型未配置？）: {}", e.getMessage());
        }
    }

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

        log.info("[AgentController] 📨 用户输入：{}", message);

        try {
            // ★ 在 HTTP 请求线程中提前获取用户（Sa-Token 需要 Request 上下文）
            User user = authService.getCurrentUser();
            String memoryId = user.getId().toString();

            // ★ 设置 ThreadLocal，让 Tool 方法能获取用户（不依赖 Sa-Token）
            SoundReadTools.setCurrentUser(user);

            String reply = agent.chat(memoryId, message);
            // 清洗模型返回内容中的内部死标记（DeepSeek think + MiniMax tool_call 等）
            reply = cleanReply(reply);

            log.info("[AgentController] 🤖 Agent 回复长度: {}", reply.length());

            return Result.ok(Map.of("reply", reply));

        } catch (Exception e) {
            log.error("[AgentController] Agent 调用失败", e);
            return Result.fail("Agent 调用失败: " + e.getMessage());
        } finally {
            // ★ 清理 ThreadLocal，防止内存泄漏
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
            log.info("[AgentController] 🗑️ 已清空用户 {} 的对话记忆", memoryId);
        }
        return Result.ok("对话已重置");
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
                        .maxTokens(1024)
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
                .maxMessages(20)
                .chatMemoryStore(memoryStore)
                .build();

        return AiServices.builder(SmartAssistant.class)
                .chatLanguageModel(toolModel)
                .tools(soundReadTools)
                .chatMemoryProvider(memoryProvider)
                .build();
    }

    /**
     * 清洗模型回复中的内部标记，防止透出到前端
     *
     * <ul>
     * <li>DeepSeek think 标签: {@code <think>...</think>}</li>
     * <li>MiniMax Tool Call 宽松格式:
     * {@code function| tool_sep |...| tool_calls_end |}</li>
     * <li>MiniMax Tool Call 紧凑格式: {@code <|tool_sep|>} {@code <|tool_calls_end|>}
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
