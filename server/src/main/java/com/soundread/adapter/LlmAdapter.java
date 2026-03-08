package com.soundread.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 豆包大模型适配器 (兼容 OpenAI Chat Completions API)
 * 
 * 豆包 API 兼容 OpenAI 格式，endpoint: https://ark.cn-beijing.volces.com/api/v3
 */
@Slf4j
@Component
public class LlmAdapter {

    private final OkHttpClient httpClient;

    @Value("${ai.llm.api-keys.doubao}")
    private String apiKey;

    @Value("${ai.llm.default-model}")
    private String modelId;

    /** 豆包 API 端点 */
    private static final String BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";
    private static final int MAX_TOKENS = 4096;

    public LlmAdapter() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 同步对话 (非流式)
     */
    public String chat(String systemPrompt, String userMessage) {
        JSONObject body = buildChatBody(systemPrompt, userMessage, false);

        Request request = new Request.Builder()
                .url(BASE_URL + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("豆包 LLM 请求失败: " + response.code());
            }
            JSONObject result = JSON.parseObject(response.body().string());
            return result.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            log.error("豆包 LLM 调用异常: ", e);
            throw new RuntimeException("AI 服务暂时不可用");
        }
    }

    /**
     * 流式对话 (SSE)
     * 
     * @param onChunk 每收到一个 token 时的回调
     */
    public void chatStream(String systemPrompt, String userMessage, Consumer<String> onChunk) {
        JSONObject body = buildChatBody(systemPrompt, userMessage, true);

        Request request = new Request.Builder()
                .url(BASE_URL + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("豆包 LLM 流式请求失败: " + response.code());
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                        String jsonStr = line.substring(6);
                        JSONObject chunk = JSON.parseObject(jsonStr);
                        String content = chunk.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("delta")
                                .getString("content");
                        if (content != null) {
                            onChunk.accept(content);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("豆包 LLM 流式调用异常: ", e);
            throw new RuntimeException("AI 服务暂时不可用");
        }
    }

    /**
     * AI 内容安全审核
     */
    public boolean moderateContent(String text) {
        String result = chat(
                "你是一个内容安全审核员。判断以下文本是否包含违规内容（色情/暴力/政治敏感/违法信息）。" +
                        "只回答 SAFE 或 UNSAFE，不要有其他输出。",
                text);
        return "SAFE".equalsIgnoreCase(result.trim());
    }

    /**
     * AI 情感分析
     * 
     * @return 情感标签列表，如 ["无奈", "激动", "疲惫"]
     */
    public List<String> analyzeEmotion(String text) {
        String result = chat(
                "你是一个情感分析专家。分析以下文本中每段对话的情绪。" +
                        "返回 JSON 数组格式，例如: [\"开心\", \"悲伤\", \"愤怒\"]，只返回数组，不要其他内容。",
                text);
        try {
            return JSON.parseArray(result.trim(), String.class);
        } catch (Exception e) {
            log.warn("情感分析结果解析失败: {}", result);
            return List.of("中性");
        }
    }

    /**
     * AI 翻译
     */
    public String translate(String text, String targetLanguage) {
        return chat(
                "你是一个专业翻译器。将以下文本翻译成" + targetLanguage + "。只输出翻译结果，不要解释。",
                text);
    }

    /**
     * AI URL 网页摘要提取
     */
    public String summarizeUrl(String pageContent) {
        return chat(
                "你是一个内容提取专家。将以下网页内容提炼为适合做播客讨论的核心要点，把控在 500 字以内。" +
                        "用自然段落形式输出，不要使用标题或列表符号。",
                pageContent);
    }

    private JSONObject buildChatBody(String systemPrompt, String userMessage, boolean stream) {
        JSONObject body = new JSONObject();
        body.put("model", modelId);
        body.put("max_tokens", MAX_TOKENS);
        body.put("stream", stream);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)));
        return body;
    }
}
