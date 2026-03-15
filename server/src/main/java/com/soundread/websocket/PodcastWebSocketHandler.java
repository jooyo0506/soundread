package com.soundread.websocket;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.soundread.adapter.R2StorageAdapter;
import com.soundread.common.exception.QuotaExceededException;
import com.soundread.model.entity.UserCreation;
import com.soundread.mapper.UserMapper;
import com.soundread.model.entity.User;
import com.soundread.sdk.podcast.PodcastClient;
import com.soundread.sdk.podcast.config.PodcastProperties;
import com.soundread.service.CreationService;
import com.soundread.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 播客 WebSocket 流式生成 Handler
 *
 * <p>
 * 前端通过 WebSocket 发送如下 JSON 开始播客生成：
 * {"action":"generate","text":"...","sourceType":"topic","voiceA":"...","voiceB":"...","headMusic":true,"tailMusic":false}
 * </p>
 *
 * <p>
 * 服务端依次推送以下事件帧：
 * <ul>
 * <li>Text({"event":"round_start","roundId":1,"speaker":"主播A","text":"..."})</li>
 * <li>Binary(音频数据) ... 可多个</li>
 * <li>Text({"event":"round_end","roundId":1,"duration":8.5})</li>
 * <li>Text({"event":"complete","audioUrl":"https://..."})</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PodcastWebSocketHandler extends TextWebSocketHandler {

    private final PodcastProperties podcastProperties;
    private final QuotaService quotaService;
    private final UserMapper userMapper;
    private final R2StorageAdapter r2StorageAdapter;
    private final CreationService creationService;
    private final com.soundread.service.TierPolicyService tierPolicyService;

    /**
     * R2 音频上传专用线程池（来自 AsyncConfig，与 WebSocket 线程隔离）
     * 注意：不能用 final+@RequiredArgsConstructor，Lombok 不低推 @Qualifier，会导致
     * NoUniqueBeanDefinitionException
     */
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("r2UploadExecutor")
    private Executor r2UploadExecutor;

    /** 活跃会话映射（WebSocket sessionId → PodcastSession） */
    private final Map<String, PodcastClient.PodcastSession> activeSessions = new ConcurrentHashMap<>();

    /** 播客生成限流：每用户 3次/60秒（WebSocket 不走 AOP，需手动限流） */
    private final Cache<Long, AtomicInteger> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(5_000)
            .build();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[Podcast WS] 连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject req = JSON.parseObject(message.getPayload());
        String action = req.getString("action");

        if (!"generate".equals(action)) {
            sendError(session, "不支持的 action: " + action);
            return;
        }

        // ===== 1. 登录校验（WebSocket 不走 SaToken 拦截器，必须手动检查）=====
        User user = resolveUser(session);
        if (user == null) {
            sendError(session, "请先登录后使用 AI 播客功能");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // ===== 2. 功能权限校验（等效 @RequireFeature，WebSocket 不走 AOP）=====
        if (!tierPolicyService.hasFeature(user.getTierCode(), "ai_podcast")) {
            sendError(session, "当前套餐未开通 AI 播客功能，请升级会员");
            return;
        }

        // ===== 3. 限流防刷（3次/60秒，WebSocket 不走 @RateLimit AOP）=====
        AtomicInteger counter = rateLimitCache.get(user.getId(), k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > 3) {
            log.warn("[Podcast WS] 用户 {} 触发播客限流", user.getId());
            sendError(session, "播客生成过于频繁，请稍后再试");
            return;
        }

        // ===== 4. 配额检查（仅检查，不扣减；扣减在生成成功后执行）=====
        try {
            quotaService.checkPodcastQuota(user);
        } catch (QuotaExceededException e) {
            sendError(session, e.getMessage());
            return;
        }

        String text = req.getString("text");
        String sourceType = req.getString("sourceType");
        String voiceA = req.getString("voiceA");
        String voiceB = req.getString("voiceB");
        boolean headMusic = req.getBooleanValue("headMusic", true);
        boolean tailMusic = req.getBooleanValue("tailMusic", false);

        // 使用默认值兜底
        if (voiceA == null || voiceA.isBlank()) {
            voiceA = podcastProperties.getDefaultVoiceA();
        }
        if (voiceB == null || voiceB.isBlank()) {
            voiceB = podcastProperties.getDefaultVoiceB();
        }
        if (sourceType == null || sourceType.isBlank()) {
            sourceType = "topic";
        }

        // 文本长度校验
        if (text != null && text.length() > podcastProperties.getMaxTextLength()) {
            sendError(session, String.format("文本过长，最多支持 %d 字", podcastProperties.getMaxTextLength()));
            return;
        }

        log.info("[Podcast WS] 开始生成: userId={} sourceType={} voiceA={} voiceB={} textLen={}",
                user.getId(), sourceType, voiceA, voiceB, text != null ? text.length() : 0);

        // 捕获 lambda 需要 final 或 effectively final
        final User currentUser = user;
        final String inputText = text;
        final String finalVoiceA = voiceA;

        // 初始化 PodcastClient 并开始流式生成
        PodcastClient client = new PodcastClient(podcastProperties);
        String sessionId = session.getId();

        // 收集每轮播客文本（用于生成 transcript）
        List<String> roundTexts = new ArrayList<>();
        // 累计总时长
        double[] totalDuration = { 0 };

        PodcastClient.PodcastSession podcastSession = client.generateStreaming(text, sourceType, voiceA, voiceB,
                headMusic, tailMusic,
                new PodcastClient.PodcastStreamCallback() {

                    @Override
                    public void onRoundStart(int roundId, String speaker, String roundText) {
                        if (!isActive(sessionId))
                            return;
                        // 收集 transcript
                        roundTexts.add(speaker + ": " + roundText);

                        JSONObject event = new JSONObject();
                        event.put("event", "round_start");
                        event.put("roundId", roundId);
                        event.put("speaker", speaker);
                        event.put("text", roundText);
                        sendText(session, event.toJSONString());
                    }

                    @Override
                    public void onAudioData(int roundId, byte[] audioData) {
                        if (!isActive(sessionId))
                            return;
                        try {
                            synchronized (session) {
                                if (session.isOpen()) {
                                    session.sendMessage(new BinaryMessage(audioData));
                                }
                            }
                        } catch (IOException e) {
                            log.warn("[Podcast WS] 音频帧发送失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onRoundEnd(int roundId, double audioDuration) {
                        if (!isActive(sessionId))
                            return;
                        totalDuration[0] += audioDuration;

                        JSONObject event = new JSONObject();
                        event.put("event", "round_end");
                        event.put("roundId", roundId);
                        event.put("duration", audioDuration);
                        sendText(session, event.toJSONString());
                    }

                    @Override
                    public void onComplete(String audioUrl) {
                        // ===== Step 1: 先扣减配额 =====
                        if (currentUser != null) {
                            try {
                                quotaService.deductPodcastQuota(currentUser);
                            } catch (Exception e) {
                                log.warn("[Podcast WS] 配额扣减失败: {}", e.getMessage());
                            }
                        }

                        // ===== Step 2: ★ 立即通知前端（不等 R2 上传完成，消除 10-30s 等待）=====
                        if (isActive(sessionId)) {
                            JSONObject event = new JSONObject();
                            event.put("event", "complete");
                            event.put("audioUrl", audioUrl); // 先给临时 URL，R2 持久化后前端以历史记录为准
                            sendText(session, event.toJSONString());
                        }

                        // ===== Step 3: ★ 异步持久化（独立 r2UploadExecutor 线程，不阻塞 WebSocket）=====
                        java.util.concurrent.CompletableFuture.runAsync(
                                () -> persistPodcast(currentUser, audioUrl, inputText,
                                        finalVoiceA, roundTexts, (int) Math.round(totalDuration[0])),
                                r2UploadExecutor).exceptionally(ex -> {
                                    log.error("[Podcast WS] 异步持久化失败", ex);
                                    return null;
                                });
                    }

                    @Override
                    public void onError(String error) {
                        if (!isActive(sessionId))
                            return;
                        sendError(session, error);
                    }
                });

        // 保存 PodcastSession 以支持取消
        activeSessions.put(sessionId, podcastSession);
    }

    /**
     * 将播客音频持久化到 R2，并保存创作记录到数据库
     */
    private String persistPodcast(User user, String upstreamUrl, String inputText,
            String voiceId, List<String> roundTexts, int durationSeconds) {
        if (user == null) {
            log.warn("[Podcast WS] 匿名用户不保存创作记录");
            return upstreamUrl;
        }

        try {
            String permanentUrl = upstreamUrl;
            long fileSize = 0;

            // 1. 下载上游临时 URL 并上传到 R2
            if (upstreamUrl != null && !upstreamUrl.isBlank()) {
                byte[] audioBytes = downloadUrl(upstreamUrl);
                fileSize = audioBytes.length;
                String filename = "podcast_" + UUID.randomUUID() + ".mp3";
                permanentUrl = r2StorageAdapter.uploadAudio(audioBytes, filename);
                log.info("[Podcast WS] 音频已转存 R2: url={} size={}", permanentUrl, fileSize);
            }

            // 2. 保存创作记录
            UserCreation creation = new UserCreation();
            creation.setUserId(user.getId());
            creation.setType("podcast");
            creation.setTitle("AI播客 · " + (inputText != null
                    ? inputText.substring(0, Math.min(inputText.length(), 30))
                    : "未命名"));
            creation.setInputText(inputText != null
                    ? inputText.substring(0, Math.min(inputText.length(), 500))
                    : "");
            creation.setVoiceId(voiceId);
            creation.setAudioUrl(permanentUrl);
            creation.setAudioDuration(Math.max(1, durationSeconds));
            creation.setFileSize(fileSize);
            // 保存 transcript 到 extraJson
            if (!roundTexts.isEmpty()) {
                String transcript = String.join("\n", roundTexts);
                creation.setExtraJson(JSON.toJSONString(Map.of(
                        "transcript", transcript.substring(0, Math.min(transcript.length(), 5000)),
                        "roundCount", roundTexts.size())));
            }
            creationService.save(creation);
            log.info("[Podcast WS] 创作记录保存成功: id={} userId={}", creation.getId(), user.getId());

            return permanentUrl;
        } catch (Exception e) {
            log.error("[Podcast WS] 持久化失败: {} ", e.getMessage(), e);
            return upstreamUrl; // 降级：返回上游临时 URL
        }
    }

    /**
     * 下载远程 URL 内容为 byte[]
     */
    private byte[] downloadUrl(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        try (InputStream in = url.openStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("[Podcast WS] 连接关闭: sessionId={} status={}", session.getId(), status);
        // 取消正在进行的生成任务
        PodcastClient.PodcastSession podcastSession = activeSessions.remove(session.getId());
        if (podcastSession != null) {
            podcastSession.cancel();
            log.info("[Podcast WS] 已取消生成任务: sessionId={}", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[Podcast WS] 传输异常: sessionId={}", session.getId(), exception);
        // 清理会话并取消任务
        PodcastClient.PodcastSession podcastSession = activeSessions.remove(session.getId());
        if (podcastSession != null) {
            podcastSession.cancel();
        }
    }

    /**
     * 从 WebSocket handshake 的 query 参数中解析当前用户
     */
    private User resolveUser(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null)
                return null;

            String query = uri.getQuery();
            if (query == null)
                return null;

            String token = null;
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && ("satoken".equals(kv[0]) || "token".equals(kv[0]))) {
                    token = kv[1];
                    break;
                }
            }

            if (token == null || token.isBlank())
                return null;

            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null)
                return null;

            return userMapper.selectById(Long.parseLong(loginId.toString()));
        } catch (Exception e) {
            log.warn("[Podcast WS] 用户身份解析失败: {}", e.getMessage());
            return null;
        }
    }

    private boolean isActive(String sessionId) {
        PodcastClient.PodcastSession ps = activeSessions.get(sessionId);
        return ps != null && !ps.isCancelled();
    }

    private void sendText(WebSocketSession session, String text) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(text));
                }
            }
        } catch (IOException e) {
            log.warn("[Podcast WS] 文本帧发送失败: {}", e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        JSONObject event = new JSONObject();
        event.put("event", "error");
        event.put("message", errorMessage);
        sendText(session, event.toJSONString());
    }

}
