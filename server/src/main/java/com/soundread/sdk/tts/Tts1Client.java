package com.soundread.sdk.tts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.soundread.sdk.common.BaseClient;
import com.soundread.sdk.common.exception.SdkException;
import com.soundread.sdk.tts.config.TtsProperties;
import com.soundread.sdk.tts.model.TtsResponse;
import com.soundread.sdk.tts.model.VoiceInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 火山引擎TTS 1.0 客户端
 * 支持短文本合成和长文本异步合成
 */
@Slf4j
public class Tts1Client extends BaseClient {

    private static final String HOST = "openspeech.bytedance.com";

    private final TtsProperties properties;

    public Tts1Client(String appId, String accessToken, String cluster) {
        super(appId, accessToken, cluster);
        this.properties = new TtsProperties();
        this.properties.setAppId(appId);
        this.properties.setAccessToken(accessToken);
        this.properties.setCluster(cluster);
    }

    public Tts1Client(TtsProperties properties) {
        super(properties.getAppId(), properties.getAccessToken(), properties.getCluster());
        this.properties = properties;
    }

    /**
     * 获取可用音色列表
     */
    public List<VoiceInfo> listVoices() {
        List<VoiceInfo> voices = new ArrayList<>();

        // 中文音色
        voices.add(createVoice("BV001_streaming", "通用女声", "zh-CN", "Female"));
        voices.add(createVoice("BV002_streaming", "通用男声", "zh-CN", "Male"));
        voices.add(createVoice("BV700_streaming", "灿灿", "zh-CN", "Female"));
        voices.add(createVoice("BV102_streaming", "儒雅青年", "zh-CN", "Male"));
        voices.add(createVoice("BV113_streaming", "甜宠少御", "zh-CN", "Female"));
        voices.add(createVoice("BV033_streaming", "温柔小哥", "zh-CN", "Male"));
        voices.add(createVoice("BV034_streaming", "知性姐姐-双语", "zh-CN", "Female"));

        // 英文音色
        voices.add(createVoice("BV503_streaming", "活力女声-Ariana", "en-US", "Female"));
        voices.add(createVoice("BV504_streaming", "活力男声-Jackson", "en-US", "Male"));

        // 日语音色
        voices.add(createVoice("BV524_streaming", "日语男声", "ja-JP", "Male"));

        return voices;
    }

    private VoiceInfo createVoice(String voiceId, String name, String locale, String gender) {
        return VoiceInfo.builder()
                .voiceId(voiceId)
                .name(name)
                .locale(locale)
                .gender(gender)
                .shortName(name)
                .supportEmotion(false)
                .build();
    }

    /**
     * 短文本语音合成 (同步)
     *
     * @param text    文本内容 (≤300字符)
     * @param voiceId 音色ID
     * @return 音频数据
     */
    public byte[] synthesize(String text, String voiceId) {
        return synthesize(text, voiceId, 1.0f, 1.0f, 1.0f);
    }

    /**
     * 短文本语音合成 (同步)
     *
     * @param text        文本内容
     * @param voiceId     音色ID
     * @param speedRatio  语速 (0.5-2.0)
     * @param volumeRatio 音量 (0.5-2.0)
     * @param pitchRatio  音调 (0.5-2.0)
     * @return 音频数据
     */
    public byte[] synthesize(String text, String voiceId, float speedRatio, float volumeRatio, float pitchRatio) {
        validateRequired(text, "text");
        validateRequired(voiceId, "voiceId");
        validateTextLength(text, properties.getShortTextMaxLength(), "text");

        log.info("TTS1 短文本合成开始: voiceId={}, textLength={}", voiceId, text.length());

        try {
            // 构建请求
            Map<String, Object> requestBody = buildRequestBody(text, voiceId, speedRatio, volumeRatio, pitchRatio);
            String reqBody = JSON.toJSONString(requestBody);

            // 构建请求头（Host + Authorization，共 2 个字段）
            Map<String, String> headers = new HashMap<>(4);
            headers.put("Host", HOST);
            headers.put("Authorization", buildAuthHeader());

            // 发送请求
            String responseBody = httpUtil.postJson(properties.getEndpoint(), reqBody, headers);

            // 解析响应
            return parseAudioResponse(responseBody);

        } catch (SdkException e) {
            throw e;
        } catch (Exception e) {
            log.error("TTS1 短文本合成失败: {}", e.getMessage(), e);
            throw new SdkException("TTS synthesis failed: " + e.getMessage(), e);
        }
    }

    /**
     * 长文本异步合成 - 创建任务
     *
     * @param text    文本内容 (≤10万字符)
     * @param voiceId 音色ID
     * @return 任务ID
     */
    public String createLongTextTask(String text, String voiceId) {
        return createLongTextTask(text, voiceId, false);
    }

    /**
     * 长文本异步合成 - 创建任务 (支持情感合成)
     *
     * @param text       文本内容
     * @param voiceId    音色ID
     * @param useEmotion 是否使用情感预测版
     * @return 任务ID
     */
    public String createLongTextTask(String text, String voiceId, boolean useEmotion) {
        validateRequired(text, "text");
        validateRequired(voiceId, "voiceId");
        validateTextLength(text, properties.getLongTextMaxLength(), "text");

        log.info("TTS1 长文本异步任务创建: voiceId={}, textLength={}, useEmotion={}",
                voiceId, text.length(), useEmotion);

        try {
            // 选择接口
            String submitUrl = useEmotion ? properties.getAsyncEmotionSubmitUrl() : properties.getAsyncSubmitUrl();
            String resourceId = useEmotion ? properties.getEmotionResourceId() : properties.getResourceId();

            // 构建请求体（appid/reqid/text/format/voice_type/sample_rate/enable_subtitle，共 7 个字段）
            Map<String, Object> requestBody = new HashMap<>(8);
            requestBody.put("appid", appId);
            requestBody.put("reqid", UUID.randomUUID().toString());
            requestBody.put("text", text);
            requestBody.put("format", "mp3");
            requestBody.put("voice_type", voiceId);
            requestBody.put("sample_rate", properties.getDefaultSampleRate());
            requestBody.put("enable_subtitle", 1);

            String reqBody = JSON.toJSONString(requestBody);

            // 构建请求头（Host + Authorization + Resource-Id，共 3 个字段）
            Map<String, String> headers = new HashMap<>(4);
            headers.put("Host", HOST);
            headers.put("Authorization", buildAuthHeader());
            headers.put("Resource-Id", resourceId);

            // 发送请求
            String responseBody = httpUtil.postJson(submitUrl, reqBody, headers);

            // 解析响应
            JSONObject json = JSON.parseObject(responseBody);
            if (json.containsKey("task_id")) {
                String taskId = json.getString("task_id");
                log.info("TTS1 长文本任务创建成功: taskId={}", taskId);
                return taskId;
            } else {
                throw new SdkException("Failed to create long text task: " + responseBody);
            }

        } catch (SdkException e) {
            throw e;
        } catch (Exception e) {
            log.error("TTS1 长文本任务创建失败: {}", e.getMessage(), e);
            throw new SdkException("Failed to create long text task: " + e.getMessage(), e);
        }
    }

    /**
     * 长文本异步合成 - 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务结果
     */
    public TtsResponse queryLongTextTask(String taskId) {
        return queryLongTextTask(taskId, false);
    }

    /**
     * 长文本异步合成 - 查询任务状态
     *
     * @param taskId     任务ID
     * @param useEmotion 是否使用情感预测版
     * @return 任务结果
     */
    public TtsResponse queryLongTextTask(String taskId, boolean useEmotion) {
        validateRequired(taskId, "taskId");

        log.info("TTS1 查询长文本任务状态: taskId={}", taskId);

        try {
            // 选择接口
            String queryUrl = useEmotion ? properties.getAsyncEmotionQueryUrl() : properties.getAsyncQueryUrl();
            String resourceId = useEmotion ? properties.getEmotionResourceId() : properties.getResourceId();

            // 构建URL
            String url = queryUrl + "?appid=" + appId + "&task_id=" + taskId;

            // 构建请求头（Host + Authorization + Resource-Id，共 3 个字段）
            Map<String, String> headers = new HashMap<>(4);
            headers.put("Host", HOST);
            headers.put("Authorization", buildAuthHeader());
            headers.put("Resource-Id", resourceId);

            // 发送请求
            String responseBody = httpUtil.get(url, headers);

            // 解析响应
            JSONObject json = JSON.parseObject(responseBody);

            int taskStatus = json.getIntValue("task_status");

            if (taskStatus == 1) {
                // 成功
                TtsResponse.Subtitles subtitles = null;
                if (json.containsKey("sentences")) {
                    var sentencesArray = json.getJSONArray("sentences");
                    String[] sentences = new String[sentencesArray.size()];
                    for (int i = 0; i < sentencesArray.size(); i++) {
                        sentences[i] = sentencesArray.getString(i);
                    }
                    subtitles = TtsResponse.Subtitles.builder()
                            .sentences(sentences)
                            .build();
                }

                return TtsResponse.taskCompleted(
                        taskId,
                        json.getString("audio_url"),
                        0, // 实际时长需要下载后获取
                        subtitles);
            } else if (taskStatus == 2) {
                // 失败
                return TtsResponse.error("TASK_FAILED", json.getString("message"));
            } else {
                // 处理中
                return TtsResponse.builder()
                        .taskId(taskId)
                        .taskStatus(TtsResponse.TaskStatus.PROCESSING)
                        .success(true)
                        .build();
            }

        } catch (SdkException e) {
            throw e;
        } catch (Exception e) {
            log.error("TTS1 查询任务状态失败: {}", e.getMessage(), e);
            throw new SdkException("Failed to query task: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String text, String voiceId,
            float speedRatio, float volumeRatio, float pitchRatio) {
        // 顶层请求体（app/user/audio/request，共 4 个字段）
        Map<String, Object> request = new HashMap<>(4);

        // App 信息（appid/cluster/token，共 3 个字段）
        Map<String, Object> app = new HashMap<>(4);
        app.put("appid", appId);
        app.put("cluster", cluster);
        app.put("token", "access_token");
        request.put("app", app);

        // User 信息（uid，共 1 个字段）
        Map<String, Object> user = new HashMap<>(2);
        user.put("uid", UUID.randomUUID().toString());
        request.put("user", user);

        // Audio 参数（voice_type/encoding/speed_ratio/volume_ratio/pitch_ratio，共 5 个字段）
        Map<String, Object> audio = new HashMap<>(8);
        audio.put("voice_type", voiceId);
        audio.put("encoding", properties.getDefaultEncoding());
        audio.put("speed_ratio", speedRatio);
        audio.put("volume_ratio", volumeRatio);
        audio.put("pitch_ratio", pitchRatio);
        request.put("audio", audio);

        // Request 参数（reqid/text/text_type/operation，共 4 个字段）
        Map<String, Object> req = new HashMap<>(4);
        req.put("reqid", UUID.randomUUID().toString());
        req.put("text", text);
        req.put("text_type", "plain");
        req.put("operation", "query");
        request.put("request", req);

        return request;
    }

    /**
     * 解析音频响应
     */
    private byte[] parseAudioResponse(String responseBody) {
        JSONObject json = JSON.parseObject(responseBody);

        // 检查错误码
        int code = json.containsKey("code") ? json.getIntValue("code") : -1;
        if (code != 0 && code != 3000) {
            String message = json.containsKey("message") ? json.getString("message") : "Unknown error";
            throw new SdkException.ApiException("TTS API error: " + message, code);
        }

        // 提取音频数据
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
            throw new SdkException("No audio data in response");
        }

        return Base64.getDecoder().decode(audioBase64);
    }
}
