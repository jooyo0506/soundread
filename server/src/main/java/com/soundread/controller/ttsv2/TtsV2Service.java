package com.soundread.controller.ttsv2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundread.adapter.R2StorageAdapter;
import com.soundread.sdk.tts.v2.protocol.EventType;
import com.soundread.sdk.tts.v2.protocol.Message;
import com.soundread.sdk.tts.v2.protocol.MsgType;
import com.soundread.sdk.tts.v2.protocol.SpeechWebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.*;

/**
 * TTS v2.0 双向流式语音合成服务
 *
 * <p>
 * 通过 WebSocket 双向流式协议与火山引擎 Seed-TTS 2.0 通信，
 * 实现带情感控制的语音合成。支持解析前端传入的
 * 全局语气指令 [#xxx]，转换为 Seed-TTS 原生的 context_texts 格式。
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsV2Service {

    private static final String ENDPOINT = "wss://openspeech.bytedance.com/api/v3/tts/bidirection";
    private final ObjectMapper objectMapper;
    private final R2StorageAdapter r2StorageAdapter;

    @Value("${volcengine.tts.app-id}")
    private String appKey;

    @Value("${volcengine.tts.access-token}")
    private String accessKey;

    private static final String RESOURCE_ID = "seed-tts-2.0";

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1500; // 基础重试延迟
    private static volatile long lastRequestTime = 0; // 防止并发请求过快
    private static final long MIN_INTERVAL_MS = 800; // 最小请求间隔

    public TtsV2Response synthesize(TtsV2Request request) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            // 确保请求间隔不要太密，避免被限流
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTime;
            if (elapsed < MIN_INTERVAL_MS) {
                Thread.sleep(MIN_INTERVAL_MS - elapsed);
            }
            lastRequestTime = System.currentTimeMillis();

            try {
                return doSynthesize(request);
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt < MAX_RETRIES && e.getMessage() != null && e.getMessage().contains("未能从火山引擎")) {
                    long delay = BASE_DELAY_MS * (1L << (attempt - 1)); // 1.5s, 3s, 6s
                    log.warn("[TTS v2] 第{}次合成无音频返回，{}ms 后重试... text={}",
                            attempt, delay,
                            request.getText() != null
                                    ? request.getText().substring(0, Math.min(30, request.getText().length()))
                                    : "null");
                    Thread.sleep(delay);
                } else {
                    throw e;
                }
            }
        }
        throw lastException;
    }

    private TtsV2Response doSynthesize(TtsV2Request request) throws Exception {
        Map<String, String> headers = Map.of(
                "X-Api-App-Key", appKey,
                "X-Api-Access-Key", accessKey,
                "X-Api-Resource-Id", RESOURCE_ID,
                "X-Api-Connect-Id", UUID.randomUUID().toString());

        SpeechWebSocketClient client = new SpeechWebSocketClient(new URI(ENDPOINT), headers);
        ByteArrayOutputStream audioStream = new ByteArrayOutputStream();

        try {
            client.connectBlocking();

            // 翻译器：将前端友好的提示词语法 [#xxx] 翻译为底层的 context_texts
            String text = request.getText();
            String contextTextStr = request.getContextText();
            List<String> contextTexts = new ArrayList<>();

            if (text != null) {
                // 提取全部全局指令 [#...] 并装入 context_texts（支持多指令）
                java.util.regex.Pattern promptPattern = java.util.regex.Pattern.compile("\\[#([^\\]]+)\\]");
                java.util.regex.Matcher promptMatcher = promptPattern.matcher(text);
                while (promptMatcher.find()) {
                    String instruction = promptMatcher.group(1).trim();
                    contextTexts.add(instruction);
                }
                // 从原文中移除所有指令标签，只保留纯文本
                text = promptPattern.matcher(text).replaceAll("").trim();
            }

            // 合并单独传递的 Context 字段（利用 SeedTTS 原生支持的多元素数组上下文特征，替代原先的字符串硬拼接）
            if (contextTextStr != null && !contextTextStr.isEmpty()) {
                // 将剧情上下文作为第一维度的前置引导
                contextTexts.add(0, contextTextStr);
            }

            // 装配音频参数
            Map<String, Object> audioParams = new HashMap<>(4);
            audioParams.put("format", "mp3");
            audioParams.put("sample_rate", 24000);
            audioParams.put("enable_timestamp", true);

            Map<String, Object> additions = new HashMap<>(4);
            additions.put("disable_markdown_filter", false);
            if (!contextTexts.isEmpty()) {
                // 将指令和上下文一并装入官方支持的 context_texts 引导参数
                additions.put("context_texts", contextTexts);
            }

            Map<String, Object> reqParamsInner = new HashMap<>(4);
            reqParamsInner.put("speaker", request.getVoiceType());
            reqParamsInner.put("audio_params", audioParams);
            reqParamsInner.put("additions", objectMapper.writeValueAsString(additions));

            Map<String, Object> req = Map.of(
                    "user", Map.of("uid", request.getUserKey()),
                    "namespace", "BidirectionalTTS",
                    "req_params", reqParamsInner);

            // 1. 发起 Connection 连接打通大模型节点
            client.sendStartConnection();
            client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.CONNECTION_STARTED);

            String sessionId = UUID.randomUUID().toString();

            // 2. 发起 Session 会话请求并装填根参数
            Map<String, Object> startReq = new HashMap<>(req);
            startReq.put("event", EventType.START_SESSION.getValue());
            client.sendStartSession(objectMapper.writeValueAsBytes(startReq), sessionId);
            client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.SESSION_STARTED);

            // 3. 将带完整前置指令格式的整段文本作为 TASK_REQUEST 一次性发送以供引擎完整识别解析
            if (text != null && !text.isEmpty()) {
                Map<String, Object> taskReqParamsInner = new HashMap<>(reqParamsInner);
                taskReqParamsInner.put("text", text);

                Map<String, Object> taskReq = new HashMap<>();
                taskReq.put("user", req.get("user"));
                taskReq.put("namespace", req.get("namespace"));
                taskReq.put("req_params", taskReqParamsInner);
                taskReq.put("event", EventType.TASK_REQUEST.getValue());

                client.sendTaskRequest(objectMapper.writeValueAsBytes(taskReq), sessionId);
            }

            // 发送完毕结束标志
            client.sendFinishSession(sessionId);

            // 4. 开始同步阻塞消费回传响应包并截取组拼音频流
            boolean audioReceived = false;
            while (true) {
                Message msg = client.receiveMessage();
                switch (msg.getType()) {
                    case FULL_SERVER_RESPONSE:
                        break;
                    case AUDIO_ONLY_SERVER:
                        if (!audioReceived && msg.getPayload() != null && msg.getPayload().length > 0) {
                            audioReceived = true;
                        }
                        if (msg.getPayload() != null) {
                            audioStream.write(msg.getPayload());
                        }
                        break;
                    case ERROR:
                        String errorPayload = new String(msg.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                        log.error("大模型引擎返回 ERROR 帧: code={}, payload={}", msg.getErrorCode(), errorPayload);
                        throw new RuntimeException("大模型引擎报错: " + errorPayload);
                    default:
                        log.warn("包含不符合预期的模型层类型返回值: {}", msg.getType());
                }
                if (msg.getEvent() == EventType.SESSION_FINISHED || msg.getEvent() == EventType.SESSION_FAILED) {
                    break;
                }
            }

            client.sendFinishConnection();

            if (!audioReceived) {
                throw new RuntimeException("未能从火山引擎双向流式协议中捕获到实质的音频返回栈");
            }

        } finally {
            client.closeBlocking();
        }

        // 5. 存储全量合成结果到兼容 S3 的 R2 存储桶并获取统一分发连接
        byte[] audioData = audioStream.toByteArray();
        String objectKey = "tts_v2_" + System.currentTimeMillis() + ".mp3";
        String audioUrl = r2StorageAdapter.uploadAudio(audioData, objectKey);

        return new TtsV2Response(audioUrl);
    }
}
