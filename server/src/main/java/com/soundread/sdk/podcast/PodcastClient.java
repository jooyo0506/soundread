package com.soundread.sdk.podcast;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.soundread.sdk.podcast.config.PodcastProperties;
import com.soundread.sdk.podcast.protocol.*;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI播客客户端
 * 基于火山引擎 Podcast TTS WebSocket 二进制协议
 */
@Slf4j
public class PodcastClient {

    private final PodcastProperties properties;

    /**
     * 流式回调接口
     */
    public interface PodcastStreamCallback {
        /** 轮次开始 */
        void onRoundStart(int roundId, String speaker, String text);

        /** 音频数据 */
        void onAudioData(int roundId, byte[] audioData);

        /** 轮次结束 */
        void onRoundEnd(int roundId, double audioDuration);

        /** 全部完成 */
        void onComplete(String audioUrl);

        /** 错误 */
        void onError(String error);
    }

    /**
     * 播客生成会话句柄，用于外部取消
     */
    public static class PodcastSession {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile InternalWsClient upstreamClient;

        /** 取消生成：立即关闭上游 WS 连接，停止 Token 消耗 */
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                log.info("PodcastSession 收到取消信号，正在关闭上游连接...");
                InternalWsClient client = upstreamClient;
                if (client != null) {
                    try {
                        client.closeBlocking();
                    } catch (Exception e) {
                        log.warn("关闭上游 WS 失败: {}", e.getMessage());
                    }
                }
            }
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        void setUpstreamClient(InternalWsClient client) {
            this.upstreamClient = client;
        }
    }

    public PodcastClient(PodcastProperties properties) {
        this.properties = properties;
    }

    /**
     * 便捷构造器 (兼容 VolcengineSdk 调用)
     */
    public PodcastClient(String appId, String accessToken) {
        this.properties = new PodcastProperties();
        this.properties.setAppId(appId);
        this.properties.setAccessToken(accessToken);
    }

    /**
     * 流式生成播客
     *
     * @param text         输入文本/主题
     * @param sourceType   来源类型: topic(主题)/link(URL)/text(原文)
     * @param voiceA       主播A音色ID
     * @param voiceB       主播B音色ID
     * @param useHeadMusic 是否使用开头音效
     * @param useTailMusic 是否使用结尾音效
     * @param callback     流式回调
     */
    public PodcastSession generateStreaming(String text, String sourceType, String voiceA, String voiceB,
            boolean useHeadMusic, boolean useTailMusic,
            PodcastStreamCallback callback) {

        PodcastSession session = new PodcastSession();

        if (text == null || text.isBlank()) {
            callback.onError("输入内容不能为空");
            return session;
        }

        new Thread(() -> {
            InternalWsClient client = null;
            boolean hasError = false;
            try {
                log.info("Podcast 线程启动, appId={}, endpoint={}", properties.getAppId(), properties.getEndpoint());

                // 构建请求头
                Map<String, String> headers = new HashMap<>();
                headers.put("X-Api-App-Id", properties.getAppId());
                headers.put("X-Api-App-Key", "aGjiRDfUWi");
                headers.put("X-Api-Access-Key", properties.getAccessToken());
                headers.put("X-Api-Resource-Id", properties.getResourceId());
                headers.put("X-Api-Connect-Id", UUID.randomUUID().toString());

                // 连接上游
                log.info("Podcast 正在连接上游 WS: {}", properties.getEndpoint());
                client = new InternalWsClient(new URI(properties.getEndpoint()), headers);
                session.setUpstreamClient(client);
                boolean connected = client.connectBlocking(properties.getConnectTimeout(), TimeUnit.SECONDS);
                if (!connected) {
                    log.error("Podcast 上游 WS 连接失败 (超时 {}s)", properties.getConnectTimeout());
                    callback.onError("上游 WebSocket 连接失败");
                    return;
                }
                if (session.isCancelled()) {
                    log.info("Podcast 连接成功但会话已取消，直接关闭");
                    return;
                }
                log.info("Podcast 上游 WS 连接成功");

                // 构建请求 payload
                JSONObject reqParams = new JSONObject();
                reqParams.put("input_id", UUID.randomUUID().toString());

                // 根据 sourceType 决定 action
                int action;
                if ("link".equals(sourceType)) {
                    action = 0;
                    reqParams.put("input_info", new JSONObject()
                            .fluentPut("input_url", text)
                            .fluentPut("return_audio_url", true));
                } else if ("text".equals(sourceType)) {
                    action = 0;
                    reqParams.put("input_text", text);
                } else {
                    // topic → 使用 action=4 (prompt 扩展)
                    action = 4;
                    reqParams.put("prompt_text", text);
                }

                reqParams.put("action", action);
                reqParams.put("use_head_music", useHeadMusic);
                reqParams.put("use_tail_music", useTailMusic);

                reqParams.put("audio_config", new JSONObject()
                        .fluentPut("format", "mp3")
                        .fluentPut("sample_rate", 24000)
                        .fluentPut("speech_rate", 0));

                if (voiceA != null && voiceB != null) {
                    reqParams.put("speaker_info", new JSONObject()
                            .fluentPut("random_order", false)
                            .fluentPut("speakers", List.of(voiceA, voiceB)));
                }

                log.info("Podcast 请求: action={}, sourceType={}, textLen={}", action, sourceType, text.length());

                // 发送 StartConnection
                sendStartConnection(client);
                log.info("Podcast → StartConnection 已发送");

                // 发送 StartSession
                String sessionId = UUID.randomUUID().toString();
                sendStartSession(client, reqParams.toJSONString().getBytes(StandardCharsets.UTF_8), sessionId);
                log.info("Podcast → StartSession 已发送, sessionId={}", sessionId);

                // 发送 FinishSession (告知服务端输入结束)
                sendFinishSession(client, sessionId);
                log.info("Podcast → FinishSession 已发送");

                // 接收响应循环
                int currentRoundId = -10000;
                String audioUrl = null;

                while (true) {
                    // 检查是否被外部取消
                    if (session.isCancelled()) {
                        log.info("Podcast 生成被用户取消，停止接收上游数据");
                        hasError = true; // 防止触发 onComplete
                        break;
                    }

                    PodcastMessage msg = client.receiveMessage(properties.getReadTimeout());
                    if (msg == null) {
                        // 可能是取消导致的超时
                        if (session.isCancelled()) {
                            log.info("Podcast 接收超时（会话已取消）");
                            hasError = true;
                            break;
                        }
                        log.error("Podcast 接收消息超时 ({}s)", properties.getReadTimeout());
                        hasError = true;
                        callback.onError("接收消息超时");
                        break;
                    }

                    log.debug("Podcast ← 收到: type={}, event={}", msg.getType(), msg.getEvent());

                    // 错误帧
                    if (msg.getType() == MsgType.ERROR) {
                        String errText = msg.getPayloadText();
                        log.error("Podcast 服务端错误: code={}, msg={}", msg.getErrorCode(), errText);
                        hasError = true;
                        callback.onError("服务端错误: " + errText);
                        break;
                    }

                    // ConnectionStarted
                    if (msg.getEvent() == EventType.CONNECTION_STARTED) {
                        log.info("Podcast ← ConnectionStarted");
                        continue;
                    }

                    // ConnectionFailed
                    if (msg.getEvent() == EventType.CONNECTION_FAILED) {
                        log.error("Podcast ← ConnectionFailed: {}", msg.getPayloadText());
                        hasError = true;
                        callback.onError("上游连接失败: " + msg.getPayloadText());
                        break;
                    }

                    // SessionStarted
                    if (msg.getEvent() == EventType.SESSION_STARTED) {
                        log.info("Podcast ← SessionStarted");
                        continue;
                    }

                    // SessionFailed
                    if (msg.getEvent() == EventType.SESSION_FAILED) {
                        log.error("Podcast ← SessionFailed: {}", msg.getPayloadText());
                        hasError = true;
                        callback.onError("会话失败: " + msg.getPayloadText());
                        break;
                    }

                    // 音频数据帧
                    if (msg.getEvent() == EventType.PODCAST_ROUND_RESPONSE) {
                        if (msg.getPayload() != null && msg.getPayload().length > 0) {
                            callback.onAudioData(currentRoundId, msg.getPayload());
                        }
                        continue;
                    }

                    // 轮次开始
                    if (msg.getEvent() == EventType.PODCAST_ROUND_START) {
                        String jsonStr = msg.getPayloadText();
                        JSONObject roundInfo = JSON.parseObject(jsonStr);
                        currentRoundId = roundInfo.getIntValue("round_id");
                        String speaker = roundInfo.getString("speaker");
                        String roundText = roundInfo.getString("text");

                        log.info("Podcast ← RoundStart: roundId={}, speaker={}", currentRoundId, speaker);
                        callback.onRoundStart(currentRoundId, speaker, roundText);
                        continue;
                    }

                    // 轮次结束
                    if (msg.getEvent() == EventType.PODCAST_ROUND_END) {
                        String jsonStr = msg.getPayloadText();
                        JSONObject endInfo = JSON.parseObject(jsonStr);

                        if (endInfo.containsKey("is_error") && endInfo.getBooleanValue("is_error")) {
                            String errMsg = endInfo.getString("error_msg");
                            log.error("Podcast ← RoundEnd 错误: {}", errMsg);
                            hasError = true;
                            callback.onError("轮次错误: " + errMsg);
                            break;
                        }

                        double audioDuration = endInfo.getDoubleValue("audio_duration");
                        log.info("Podcast ← RoundEnd: roundId={}, duration={}s", currentRoundId, audioDuration);
                        callback.onRoundEnd(currentRoundId, audioDuration);
                        continue;
                    }

                    // 播客结束
                    if (msg.getEvent() == EventType.PODCAST_END) {
                        String jsonStr = msg.getPayloadText();
                        if (jsonStr != null) {
                            JSONObject endInfo = JSON.parseObject(jsonStr);
                            JSONObject metaInfo = endInfo.getJSONObject("meta_info");
                            if (metaInfo != null) {
                                audioUrl = metaInfo.getString("audio_url");
                            }
                        }
                        log.info("Podcast ← PodcastEnd, audioUrl={}", audioUrl);
                        continue;
                    }

                    // 会话结束
                    if (msg.getEvent() == EventType.SESSION_FINISHED) {
                        log.info("Podcast ← SessionFinished (正常结束)");
                        break;
                    }

                    // 用量事件
                    if (msg.getEvent() == EventType.USAGE_RESPONSE) {
                        log.info("Podcast ← UsageResponse: {}", msg.getPayloadText());
                        continue;
                    }

                    // 未知事件
                    log.warn("Podcast ← 未处理事件: type={}, event={}, payload={}", msg.getType(), msg.getEvent(),
                            msg.getPayloadText());
                }

                // 发送 FinishConnection
                try {
                    sendFinishConnection(client);
                } catch (Exception e) {
                    log.warn("Podcast → FinishConnection 发送失败: {}", e.getMessage());
                }

                // 只有没出错才通知完成
                if (!hasError) {
                    log.info("Podcast 生成成功完成");
                    callback.onComplete(audioUrl);
                }

            } catch (Exception e) {
                log.error("Podcast 生成异常", e);
                callback.onError("生成异常: " + e.getMessage());
            } finally {
                if (client != null) {
                    try {
                        client.closeBlocking();
                    } catch (Exception ignored) {
                    }
                }
            }
        }, "podcast-stream-" + System.currentTimeMillis()).start();

        return session;
    }

    // ==================== 协议发送方法 ====================

    private void sendStartConnection(InternalWsClient client) throws Exception {
        PodcastMessage msg = new PodcastMessage(MsgType.FULL_CLIENT_REQUEST, MsgTypeFlagBits.WITH_EVENT);
        msg.setEvent(EventType.START_CONNECTION);
        msg.setPayload("{}".getBytes(StandardCharsets.UTF_8));
        client.send(msg.marshal());
    }

    private void sendStartSession(InternalWsClient client, byte[] payload, String sessionId) throws Exception {
        PodcastMessage msg = new PodcastMessage(MsgType.FULL_CLIENT_REQUEST, MsgTypeFlagBits.WITH_EVENT);
        msg.setEvent(EventType.START_SESSION);
        msg.setSessionId(sessionId);
        msg.setPayload(payload);
        client.send(msg.marshal());
    }

    private void sendFinishSession(InternalWsClient client, String sessionId) throws Exception {
        PodcastMessage msg = new PodcastMessage(MsgType.FULL_CLIENT_REQUEST, MsgTypeFlagBits.WITH_EVENT);
        msg.setEvent(EventType.FINISH_SESSION);
        msg.setSessionId(sessionId);
        msg.setPayload("{}".getBytes(StandardCharsets.UTF_8));
        client.send(msg.marshal());
    }

    private void sendFinishConnection(InternalWsClient client) throws Exception {
        PodcastMessage msg = new PodcastMessage(MsgType.FULL_CLIENT_REQUEST, MsgTypeFlagBits.WITH_EVENT);
        msg.setEvent(EventType.FINISH_CONNECTION);
        client.send(msg.marshal());
    }

    // ==================== 音色相关 ====================

    /** 获取可用音色 */
    public List<PodcastVoice> listVoices() {
        List<PodcastVoice> voices = new ArrayList<>();
        voices.add(new PodcastVoice("zh_female_mizaitongxue_v2_saturn_bigtts", "咪仔", "female", "黑猫侦探社"));
        voices.add(new PodcastVoice("zh_male_dayixiansheng_v2_saturn_bigtts", "大一先生", "male", "黑猫侦探社"));
        voices.add(new PodcastVoice("zh_male_liufei_v2_saturn_bigtts", "刘飞", "male", "科技商业"));
        voices.add(new PodcastVoice("zh_male_xiaolei_v2_saturn_bigtts", "潇磊", "male", "科技商业"));
        return voices;
    }

    /** 获取推荐预设 */
    public List<VoicePreset> getRecommendedPresets() {
        return List.of(VoicePreset.DETECTIVE, VoicePreset.TECH);
    }

    // ==================== 内部类 ====================

    @lombok.Data
    public static class PodcastVoice {
        private String voiceId;
        private String name;
        private String gender;
        private String series;

        public PodcastVoice(String voiceId, String name, String gender, String series) {
            this.voiceId = voiceId;
            this.name = name;
            this.gender = gender;
            this.series = series;
        }
    }

    public enum VoicePreset {
        DETECTIVE("zh_female_mizaitongxue_v2_saturn_bigtts",
                "zh_male_dayixiansheng_v2_saturn_bigtts",
                "黑猫侦探社系列", "咪仔 & 大一先生"),
        TECH("zh_male_liufei_v2_saturn_bigtts",
                "zh_male_xiaolei_v2_saturn_bigtts",
                "科技商业脱口秀", "刘飞 & 潇磊");

        private final String voiceA;
        private final String voiceB;
        private final String name;
        private final String desc;

        VoicePreset(String voiceA, String voiceB, String name, String desc) {
            this.voiceA = voiceA;
            this.voiceB = voiceB;
            this.name = name;
            this.desc = desc;
        }

        public String getVoiceA() {
            return voiceA;
        }

        public String getVoiceB() {
            return voiceB;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }
    }

    /**
     * 内部 WebSocket 客户端 — 连接火山引擎上游
     */
    private static class InternalWsClient extends WebSocketClient {
        private final BlockingQueue<PodcastMessage> messageQueue = new LinkedBlockingQueue<>();

        public InternalWsClient(URI serverUri, Map<String, String> headers) {
            super(serverUri, headers);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            log.info("Podcast upstream WS 已连接, logid={}", handshake.getFieldValue("x-tt-logid"));
        }

        @Override
        public void onMessage(String message) {
            log.warn("Podcast upstream 收到意外文本消息: {}", message);
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            try {
                PodcastMessage message = PodcastMessage.unmarshal(bytes.array());
                messageQueue.put(message);
            } catch (Exception e) {
                log.error("Podcast 消息解析失败", e);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            log.info("Podcast upstream WS 关闭: code={}, reason={}", code, reason);
        }

        @Override
        public void onError(Exception ex) {
            log.error("Podcast upstream WS 错误", ex);
        }

        public PodcastMessage receiveMessage(int timeoutSeconds) throws InterruptedException {
            return messageQueue.poll(timeoutSeconds, TimeUnit.SECONDS);
        }
    }
}
