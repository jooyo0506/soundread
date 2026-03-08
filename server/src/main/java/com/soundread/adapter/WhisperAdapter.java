package com.soundread.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Whisper ASR 语音识别适配器
 * 用于：边听边问语音转文字、字幕生成、音频质量检测
 */
@Slf4j
@Component
public class WhisperAdapter {

    private final OkHttpClient httpClient;

    @Value("${ai.whisper.api-key}")
    private String apiKey;

    @Value("${ai.whisper.base-url}")
    private String baseUrl;

    public WhisperAdapter() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 语音转文字 (ASR)
     * 
     * @param audioData 音频字节数据
     * @param filename  文件名 (带扩展名)
     * @return 识别后的文本
     */
    public String transcribe(byte[] audioData, String filename) {
        RequestBody fileBody = RequestBody.create(audioData, MediaType.parse("audio/wav"));
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, fileBody)
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("language", "zh")
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("Whisper ASR 请求失败: " + response.code());
            }
            JSONObject result = JSON.parseObject(response.body().string());
            return result.getString("text");
        } catch (Exception e) {
            log.error("Whisper ASR 异常: ", e);
            throw new RuntimeException("语音识别服务暂时不可用");
        }
    }

    /**
     * 生成带时间戳的字幕 (SRT 格式)
     */
    public String generateSubtitles(byte[] audioData, String filename) {
        RequestBody fileBody = RequestBody.create(audioData, MediaType.parse("audio/mp3"));
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, fileBody)
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("language", "zh")
                .addFormDataPart("response_format", "srt")
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("字幕生成请求失败: " + response.code());
            }
            return response.body().string();
        } catch (Exception e) {
            log.error("字幕生成异常: ", e);
            throw new RuntimeException("字幕生成服务暂时不可用");
        }
    }

    /**
     * 检测音频质量 (用于声音克隆样本质量评估)
     * 返回 0-100 的质量评分
     */
    public int detectAudioQuality(byte[] audioData, String filename) {
        try {
            String text = transcribe(audioData, filename);
            if (text == null || text.isBlank())
                return 10;
            // 基础质量评估: 转写文本越完整、越清晰，质量越高
            int score = 50;
            if (text.length() > 20)
                score += 20; // 有足够内容
            if (text.length() > 50)
                score += 15; // 内容丰富
            if (!text.contains("..."))
                score += 10; // 没有断句
            if (!text.contains("[") && !text.contains("("))
                score += 5; // 无噪音标记
            return Math.min(score, 100);
        } catch (Exception e) {
            log.warn("音频质量检测失败，返回默认分值: ", e);
            return 60;
        }
    }
}
