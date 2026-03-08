package com.soundread.websocket;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.soundread.mapper.UserMapper;
import com.soundread.model.entity.User;
import com.soundread.service.AiInteractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * "边听边问" AI 语音交互 WebSocket Handler
 *
 * <p>
 * 安全: 连接建立时强制校验 Sa-Token，未登录或 Token 无效则立即关闭连接。
 * Token 通过 URL query params 传递：wss://host/ws/interaction?satoken={token}
 *
 * <p>
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

    /** session 属性 Key：已认证的 userId */
    private static final String ATTR_USER_ID = "userId";

    private final AiInteractionService aiInteractionService;
    private final UserMapper userMapper;

    public InteractionWebSocketHandler(AiInteractionService aiInteractionService,
            UserMapper userMapper) {
        this.aiInteractionService = aiInteractionService;
        this.userMapper = userMapper;
    }

    /**
     * 连接建立时校验 Sa-Token，未通过认证则直接关闭连接。
     *
     * <p>
     * 前端连接方式：
     * 
     * <pre>
     * new WebSocket(`wss://host/ws/interaction?satoken=${token}`)
     * </pre>
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);

        if (token == null || token.isBlank()) {
            log.warn("[InteractionWS] 拒绝连接：缺少 satoken 参数, sessionId={}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("缺少认证 Token"));
            return;
        }

        // 用 Sa-Token 校验 Token 是否有效（不改变当前线程上下文）
        Object loginId;
        try {
            loginId = StpUtil.getLoginIdByToken(token);
        } catch (NotLoginException e) {
            log.warn("[InteractionWS] 拒绝连接：Token 无效或已过期, sessionId={}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Token 无效或已过期"));
            return;
        }

        if (loginId == null) {
            log.warn("[InteractionWS] 拒绝连接：Token 对应用户不存在, sessionId={}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Token 无效"));
            return;
        }

        // 将 userId 存入 session，后续 handler 直接读取，无需再解析 Token
        long userId = Long.parseLong(loginId.toString());
        session.getAttributes().put(ATTR_USER_ID, userId);

        log.info("[InteractionWS] 连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // 连接未通过认证时（已被关闭），不处理任何消息
        if (!session.isOpen()) {
            return;
        }
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未认证连接"));
            return;
        }

        byte[] audioData = message.getPayload().array();
        log.info("[InteractionWS] 收到语音消息: userId={}, bytes={}", userId, audioData.length);

        try {
            // TODO: 接入真实 ASR → LLM → TTS 链路
            // User user = userMapper.selectById(userId);
            // String answer = aiInteractionService.processInteraction(user, audioData,
            // contextText, sessionId);

            // 1. 发送转写结果
            JSONObject transcribedEvent = new JSONObject(4);
            transcribedEvent.put("event", "transcribed");
            transcribedEvent.put("question", "[ASR 转写结果]");
            session.sendMessage(new TextMessage(transcribedEvent.toJSONString()));

            // 2. 发送 AI 回答
            JSONObject answeredEvent = new JSONObject(4);
            answeredEvent.put("event", "answered");
            answeredEvent.put("answer", "[AI 回答]");
            session.sendMessage(new TextMessage(answeredEvent.toJSONString()));

            // 3. 发送 TTS 音频（TODO: 接入真实合成）
            // session.sendMessage(new BinaryMessage(ttsAudioData));

            // 4. 完成信号
            JSONObject completeEvent = new JSONObject(2);
            completeEvent.put("event", "complete");
            session.sendMessage(new TextMessage(completeEvent.toJSONString()));

        } catch (Exception e) {
            log.error("[InteractionWS] 处理语音消息异常: userId={}", userId, e);
            JSONObject errorEvent = new JSONObject(4);
            errorEvent.put("event", "error");
            errorEvent.put("message", "服务端处理异常，请重试");
            session.sendMessage(new TextMessage(errorEvent.toJSONString()));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            return;
        }
        // 接收上下文信息（如 contextWorkId, sessionId）
        JSONObject context = JSON.parseObject(message.getPayload());
        session.getAttributes().put("contextWorkId", context.getLongValue("contextWorkId"));
        session.getAttributes().put("sessionId", context.getString("sessionId"));
        log.info("[InteractionWS] 设置交互上下文: userId={}, context={}", userId, context);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        log.info("[InteractionWS] 连接关闭: userId={}, sessionId={}, status={}", userId, session.getId(), status);
    }

    /**
     * 从 WebSocket 握手 URI 的 query params 中提取 satoken。
     *
     * <p>
     * 前端连接示例：wss://host/ws/interaction?satoken=xxx
     *
     * @param session WebSocket 会话
     * @return token 字符串，无参数则返回 null
     */
    private String extractToken(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        String query = session.getUri().getQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "satoken".equalsIgnoreCase(kv[0])) {
                return kv[1].trim();
            }
        }
        return null;
    }
}
