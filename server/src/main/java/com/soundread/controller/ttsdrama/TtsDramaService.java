package com.soundread.controller.ttsdrama;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsDramaService {

    private static final String ENDPOINT = "wss://openspeech.bytedance.com/api/v3/tts/bidirection";
    private final ObjectMapper objectMapper;
    private final R2StorageAdapter r2StorageAdapter;

    @Value("${volcengine.tts.app-id}")
    private String appKey;

    @Value("${volcengine.tts.access-token}")
    private String accessKey;

    private static final String RESOURCE_ID = "seed-tts-2.0";

    public TtsDramaResponse synthesizeDrama(TtsDramaRequest request) throws Exception {
        Map<String, String> headers = Map.of(
                "X-Api-App-Key", appKey,
                "X-Api-Access-Key", accessKey,
                "X-Api-Resource-Id", RESOURCE_ID,
                "X-Api-Connect-Id", UUID.randomUUID().toString());

        SpeechWebSocketClient client = new SpeechWebSocketClient(new URI(ENDPOINT), headers);
        ByteArrayOutputStream finalMixStream = new ByteArrayOutputStream();

        try {
            client.connectBlocking();

            // 1. 发起全局长连接
            client.sendStartConnection();
            client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.CONNECTION_STARTED);

            String previousSessionId = null;

            // 2. 依次按角色串行发生对话
            for (TtsDramaRequest.DialogLine line : request.getLines()) {
                if (line.getContent() == null || line.getContent().isEmpty()) {
                    continue; // 忽略空台词
                }

                String currentSessionId = UUID.randomUUID().toString();
                String text = line.getContent();
                List<String> context_texts = new ArrayList<>();

                // 解析指令语法 [#xxx]
                java.util.regex.Pattern promptPattern = java.util.regex.Pattern.compile("\\[#([^\\]]+)\\]");
                java.util.regex.Matcher promptMatcher = promptPattern.matcher(text);
                if (promptMatcher.find()) {
                    String instruction = promptMatcher.group(1).trim();
                    context_texts.add(instruction);
                    text = promptMatcher.replaceAll("").trim();
                }

                // 合并全局背景设定
                if (request.getGlobalContext() != null && !request.getGlobalContext().isEmpty()) {
                    if (context_texts.isEmpty()) {
                        context_texts.add("背景基调：" + request.getGlobalContext());
                    } else {
                        String existingInst = context_texts.get(0);
                        context_texts.set(0, "背景基调：" + request.getGlobalContext() + "。角色指导：" + existingInst);
                    }
                }

                Map<String, Object> audioParams = new HashMap<>();
                audioParams.put("format", "mp3");
                audioParams.put("sample_rate", 24000);
                audioParams.put("enable_timestamp", true);

                Map<String, Object> additions = new HashMap<>();
                additions.put("disable_markdown_filter", false);
                if (!context_texts.isEmpty()) {
                    additions.put("context_texts", context_texts);
                }
                if (previousSessionId != null) {
                    additions.put("section_id", previousSessionId); // 让大模型串联上一句上下文！
                }

                Map<String, Object> reqParamsInner = new HashMap<>();
                reqParamsInner.put("speaker", line.getSpeakerVoiceType());
                reqParamsInner.put("audio_params", audioParams);
                reqParamsInner.put("additions", objectMapper.writeValueAsString(additions));

                Map<String, Object> req = Map.of(
                        "user", Map.of("uid", request.getUserKey() != null ? request.getUserKey() : "drama-user"),
                        "namespace", "BidirectionalTTS",
                        "req_params", reqParamsInner);

                // 发起本轮对话 Session
                Map<String, Object> startReq = new HashMap<>(req);
                startReq.put("event", EventType.START_SESSION.getValue());
                client.sendStartSession(objectMapper.writeValueAsBytes(startReq), currentSessionId);
                client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.SESSION_STARTED);

                // 投递文本
                Map<String, Object> taskReqParamsInner = new HashMap<>(reqParamsInner);
                taskReqParamsInner.put("text", text);
                Map<String, Object> taskReq = new HashMap<>();
                taskReq.put("user", req.get("user"));
                taskReq.put("namespace", req.get("namespace"));
                taskReq.put("req_params", taskReqParamsInner);
                taskReq.put("event", EventType.TASK_REQUEST.getValue());

                client.sendTaskRequest(objectMapper.writeValueAsBytes(taskReq), currentSessionId);
                client.sendFinishSession(currentSessionId);

                // 阻塞接收该人的所有语音分片
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
                                finalMixStream.write(msg.getPayload()); // 源源不断写入总流
                            }
                            break;
                        case ERROR:
                            String errorPayload = new String(msg.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                            log.error("Drama引擎ERROR: code={}, msg={}", msg.getErrorCode(), errorPayload);
                            throw new RuntimeException("配音发生错误: " + errorPayload);
                        default:
                            break;
                    }
                    if (msg.getEvent() == EventType.SESSION_FINISHED || msg.getEvent() == EventType.SESSION_FAILED) {
                        break;
                    }
                }

                // 交接火炬给下一位对话人
                previousSessionId = currentSessionId;
            }

            // 拆网线
            client.sendFinishConnection();

        } finally {
            client.closeBlocking();
        }

        byte[] audioData = finalMixStream.toByteArray();
        if (audioData.length == 0) {
            throw new RuntimeException("剧情中没有任何有效返回的音频片段");
        }

        // 上传到统一存储并返回长连接
        String objectKey = "tts_drama_" + System.currentTimeMillis() + ".mp3";
        String audioUrl = r2StorageAdapter.uploadAudio(audioData, objectKey);

        return new TtsDramaResponse(audioUrl);
    }
}
