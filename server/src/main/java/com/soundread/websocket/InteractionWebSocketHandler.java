package com.soundread.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.soundread.model.entity.User;
import com.soundread.service.AiInteractionService;
import com.soundread.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * "边听边问" AI 语音交互 WebSocket Handler
 * 
 * 闭环协议:
 * → 客户端: Binary(用户语音) + Text({"contextWorkId":123,"sessionId":"abc"})
 * ← 服务端: Text({"event":"transcribed","question":"用户问题"})
 * Text({"event":"answered","answer":"AI回答"})
 * Binary(TTS合成的回答音频)
 * Text({"event":"complete"})
 */
@Slf4j
@Component
public class InteractionWebSocketHandler extends BinaryWebSocketHandler {

    private final AiInteractionService aiInteractionService;
    private final AuthService authService;

    public InteractionWebSocketHandler(AiInteractionService aiInteractionService,
            AuthService authService) {
        this.aiInteractionService = aiInteractionService;
        this.authService = authService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Interaction WebSocket 连接建立: {}", session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] audioData = message.getPayload().array();
        log.info("收到语音消息: {} bytes", audioData.length);

        try {
            // TODO: 从 session 的 handshake headers / query params 中提取 token 校验用户
            // User user = authService.getCurrentUser();

            // 模拟处理: ASR → LLM → TTS
            // String answer = aiInteractionService.processInteraction(user, audioData,
            // contextText, sessionId);

            // 1. 发送转写结果
            JSONObject transcribedEvent = new JSONObject();
            transcribedEvent.put("event", "transcribed");
            transcribedEvent.put("question", "[ASR 转写结果]");
            session.sendMessage(new TextMessage(transcribedEvent.toJSONString()));

            // 2. 发送 AI 回答
            JSONObject answeredEvent = new JSONObject();
            answeredEvent.put("event", "answered");
            answeredEvent.put("answer", "[AI 回答]");
            session.sendMessage(new TextMessage(answeredEvent.toJSONString()));

            // 3. 发送 TTS 合成的回答音频
            // session.sendMessage(new BinaryMessage(ttsAudioData));

            // 4. 完成
            JSONObject completeEvent = new JSONObject();
            completeEvent.put("event", "complete");
            session.sendMessage(new TextMessage(completeEvent.toJSONString()));

        } catch (Exception e) {
            JSONObject errorEvent = new JSONObject();
            errorEvent.put("event", "error");
            errorEvent.put("message", e.getMessage());
            session.sendMessage(new TextMessage(errorEvent.toJSONString()));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 接收上下文信息 (如 contextWorkId, sessionId)
        JSONObject context = JSON.parseObject(message.getPayload());
        session.getAttributes().put("contextWorkId", context.getLongValue("contextWorkId"));
        session.getAttributes().put("sessionId", context.getString("sessionId"));
        log.info("设置交互上下文: {}", context);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Interaction WebSocket 连接关闭: {}", session.getId());
    }
}
