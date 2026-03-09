package com.soundread.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.soundread.sdk.tts.model.TtsResponse; // Changed from com.soundread.model.entity.TtsResponse based on original import
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * TTS 1.0 HTTP 同步合成适配器（火山引擎）
 *
 * <p>
 * 封装火山引擎短文 TTS 接口和异步长文任务接口，提供同步合成和异步轮询两种方式。
 * </p>
 *
 * <p>
 * 核心接口：
 * </p>
 * <ul>
 * <li>短文合成: {@code POST https://openspeech.bytedance.com/api/v1/tts}
 * <br>
 * 请求包含 {@code app / user / audio / request} 四个结构体，返回
 * {@code { code: 3000, data: "base64mp3" }}</li>
 * <li>长文异步据交：{@code POST ...tts_async/submit}，返回 taskId</li>
 * <li>异步状态轮询：{@code POST ...tts_async/query}，返回音频 URL</li>
 * </ul>
 *
 * @author SoundRead
 */
@Slf4j
@Component
public class Tts1Adapter {

    private static final String TTS_ENDPOINT = "https://openspeech.bytedance.com/api/v1/tts";
    private static final String HOST = "openspeech.bytedance.com";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    @Value("${volcengine.tts.app-id}")
    private String appId;

    @Value("${volcengine.tts.access-token}")
    private String accessToken;

    @Value("${volcengine.tts.cluster:volcano_tts}")
    private String cluster;

    private final OkHttpClient httpClient;

    public Tts1Adapter() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 短文合成完整参数版本
     *
     * @param text        待合成文本（建议 500 字以内，超长可能超时）
     * @param voiceId     音色 ID，如 BV001_streaming
     * @param speedRatio  语速比（0.5~2.0，默认 1.0）
     * @param volumeRatio 音量比（0.5~2.0，默认 1.0）
     * @param pitchRatio  音调比（0.5~2.0，默认 1.0）
     * @return MP3 格式音频字节数组
     */
    public byte[] synthesize(String text, String voiceId, float speedRatio, float volumeRatio, float pitchRatio) {
        log.info("[TTS1] 开始合成: voiceId={} textLen={} speedRatio={}", voiceId, text.length(), speedRatio);

        // 1. 构建请求 JSON 体
        Map<String, Object> body = new LinkedHashMap<>();

        // app
        Map<String, Object> app = new LinkedHashMap<>();
        app.put("appid", appId);
        app.put("cluster", cluster);
        app.put("token", accessToken);
        body.put("app", app);

        // user
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("uid", UUID.randomUUID().toString());
        body.put("user", user);

        // audio
        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("voice_type", voiceId);
        audio.put("encoding", "mp3");
        audio.put("speed_ratio", speedRatio);
        audio.put("volume_ratio", volumeRatio);
        audio.put("pitch_ratio", pitchRatio);
        body.put("audio", audio);

        // request
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("reqid", UUID.randomUUID().toString());
        req.put("text", text);
        req.put("text_type", "plain");
        req.put("operation", "query");
        body.put("request", req);

        String jsonBody = JSON.toJSONString(body);

        // 2. 发起 HTTP 请求
        Request request = new Request.Builder()
                .url(TTS_ENDPOINT)
                .addHeader("Host", HOST)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer; " + accessToken)
                .post(RequestBody.create(jsonBody, JSON_TYPE))
                .build();

        // 3. 处理响应
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("TTS1 HTTP 请求失败 code=" + response.code());
            }

            String respBody = response.body().string();
            JSONObject json = JSON.parseObject(respBody);

            // 火山引擎返回 code=3000 表示成功，code=0 平台兼容
            int code = json.containsKey("code") ? json.getIntValue("code") : -1;
            if (code != 0 && code != 3000) {
                String message = json.containsKey("message") ? json.getString("message") : "Unknown error";
                log.error("[TTS1] 引擎返回异常 code={} message={}", code, message);
                throw new RuntimeException("TTS1 引擎异常: " + message + " (code=" + code + ")");
            }

            // 解析 base64 音频数据
            String audioBase64 = null;
            if (json.containsKey("data")) {
                audioBase64 = json.getString("data");
            } else if (json.containsKey("result")) {
                JSONObject result = json.getJSONObject("result");
                if (result != null && result.containsKey("data")) {
                    audioBase64 = result.getString("data");
                }
            }

            if (audioBase64 == null || audioBase64.isEmpty()) {
                throw new RuntimeException("TTS1 响应中无音频数据（data 字段为空）");
            }

            byte[] audioBytes = Base64.getDecoder().decode(audioBase64);
            log.info("[TTS1] 合成成功: 音频大小={} KB", audioBytes.length / 1024);
            return audioBytes;

        } catch (IOException e) {
            log.error("[TTS1] 网络请求异常: {}", e.getMessage(), e);
            throw new RuntimeException("TTS1 合成请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 短文合成默认参数入口（流速、音量、音调均按默认值 1.0）
     */
    public byte[] synthesize(String text, String voiceId) {
        return synthesize(text, voiceId, 1.0f, 1.0f, 1.0f);
    }

    /**
     * 提交长文异步合成任务，返回 taskId
     *
     * @param text       待合成文本
     * @param voiceId    音色 ID
     * @param useEmotion 是否启用情感模式（当前预留参数）
     * @return taskId
     */
    public String createLongTextTask(String text, String voiceId, boolean useEmotion) {
        log.info("[TTS1] 提交异步任务: voiceId={} textLen={} useEmotion={}",
                voiceId, text.length(), useEmotion);

        // 使用火山引擎中的 default 异步 TTS 资源
        String submitUrl = "https://openspeech.bytedance.com/api/v1/tts_async/submit";
        String resourceId = "volc.tts_async.default";
        String reqid = UUID.randomUUID().toString();

        Map<String, Object> requestBody = new HashMap<>(8);
        requestBody.put("appid", appId);
        requestBody.put("reqid", reqid);
        requestBody.put("text", text);
        requestBody.put("format", "mp3");
        requestBody.put("voice_type", voiceId);
        requestBody.put("sample_rate", 24000);

        String jsonBody = JSON.toJSONString(requestBody);

        Request request = new Request.Builder()
                .url(submitUrl)
                .addHeader("Host", HOST)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer; " + accessToken)
                .addHeader("Resource-Id", resourceId)
                .post(RequestBody.create(jsonBody, JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errBody = response.body() != null ? response.body().string() : "No body";
                log.error("[TTS1] 提交任务 HTTP 失败: code={} body={}", response.code(), errBody);
                throw new RuntimeException("TTS1 提交异步任务失败 code=" + response.code() + ", detail: " + errBody);
            }

            String respBody = response.body().string();
            JSONObject json = JSON.parseObject(respBody);

            int code = json.containsKey("code") ? json.getIntValue("code") : -1;
            if (code != 3000) {
                String message = json.getString("message");
                log.error("[TTS1] 提交任务引擎异常: code={} message={}", code, message);
                throw new RuntimeException("TTS1 任务提交失败: " + message);
            }

            // 优先使用响应中的 task_id，如果为 null 则用 reqid 兼容
            String taskId = json.getString("task_id");
            if (taskId == null) {
                taskId = reqid; // 兜底使用 reqid
            }

            log.info("[TTS1] 异步任务提交成功: taskId={}", taskId);
            return taskId;
        } catch (IOException e) {
            log.error("[TTS1] 提交任务网络异常: {}", e.getMessage(), e);
            throw new RuntimeException("TTS1 提交任务请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询异步 TTS 任务的当前状态
     *
     * @param taskId     任务 ID
     * @param useEmotion 是否情感模式（预留参数）
     * @return TtsResponse 包含状态和音频 URL
     */
    public TtsResponse queryLongTextTask(String taskId, boolean useEmotion) {
        log.info("[TTS1] 查询异步任务状态: taskId={}", taskId);

        String queryUrl = "https://openspeech.bytedance.com/api/v1/tts_async/query";
        String resourceId = "volc.tts_async.default";

        Map<String, Object> requestBody = new HashMap<>(4);
        requestBody.put("appid", appId);
        requestBody.put("task_id", taskId);

        String jsonBody = JSON.toJSONString(requestBody);

        Request request = new Request.Builder()
                .url(queryUrl)
                .addHeader("Host", HOST)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer; " + accessToken)
                .addHeader("Resource-Id", resourceId)
                .post(RequestBody.create(jsonBody, JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errBody = response.body() != null ? response.body().string() : "No body";
                log.error("[TTS1] 查询任务 HTTP 失败: code={} body={}", response.code(), errBody);
                throw new RuntimeException("TTS1 查询任务失败 code=" + response.code() + ", detail: " + errBody);
            }

            String respBody = response.body().string();
            JSONObject json = JSON.parseObject(respBody);

            int code = json.containsKey("code") ? json.getIntValue("code") : -1;
            if (code != 3000) {
                String message = json.getString("message");
                return TtsResponse.error(String.valueOf(code), message);
            }

            int status = json.getIntValue("task_status");
            // 0: 队列中 1: 进行中 2: 已完成 3: 失败
            if (status == 2) {
                String audioUrl = json.getString("audio_url");
                return TtsResponse.taskCompleted(taskId, audioUrl, 0, null);
            } else if (status == 3) {
                String errorMsg = json.containsKey("message") ? json.getString("message") : "";
                return TtsResponse.error("TASK_FAILED", errorMsg);
            }

            // 状态为进行中（未完成），返回任务已创建的状态
            return TtsResponse.taskCreated(taskId, null, 0);

        } catch (IOException e) {
            log.error("[TTS1] 查询任务网络异常: {}", e.getMessage(), e);
            return TtsResponse.error("NETWORK_ERROR", e.getMessage());
        }
    }
}
