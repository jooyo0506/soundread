package com.soundread.sdk.mureka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Mureka AI 音乐 API 客户端
 *
 * <p>
 * 封装 Mureka API 调用：歌曲生成、纯音乐生成、歌词生成、任务查询。
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Component
public class MurekaClient {

    @Value("${mureka.api-key:}")
    private String apiKey;

    @Value("${mureka.base-url:https://api.mureka.cn}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成歌曲
     *
     * @param lyrics 歌词
     * @param prompt 风格提示词
     * @param model  模型 (auto / mureka-7.5 / mureka-o1)
     * @return Mureka 任务 ID
     */
    public String generateSong(String lyrics, String prompt, String model) {
        var body = new java.util.HashMap<String, Object>();
        body.put("lyrics", lyrics);
        body.put("prompt", prompt != null ? prompt : "");
        body.put("model", model != null ? model : "auto");
        body.put("n", 1);
        body.put("stream", true);
        JsonNode resp = post("/v1/song/generate", body);
        return resp.get("id").asText();
    }

    /**
     * 生成纯音乐
     *
     * @param prompt 风格提示词
     * @param model  模型
     * @return Mureka 任务 ID
     */
    public String generateInstrumental(String prompt, String model) {
        var body = new java.util.HashMap<String, Object>();
        body.put("prompt", prompt);
        body.put("model", model != null ? model : "auto");
        body.put("n", 1);
        body.put("stream", true);
        JsonNode resp = post("/v1/instrumental/generate", body);
        return resp.get("id").asText();
    }

    /**
     * AI 生成歌词
     *
     * @param prompt 歌词描述
     * @return { title, lyrics }
     */
    public JsonNode generateLyrics(String prompt) {
        var body = Map.of("prompt", prompt);
        return post("/v1/lyrics/generate", body);
    }

    /**
     * 查询歌曲任务状态
     *
     * @param taskId Mureka 任务 ID
     * @return 完整的任务状态 JSON
     */
    public JsonNode querySongTask(String taskId) {
        return get("/v1/song/query/" + taskId);
    }

    /**
     * 查询纯音乐任务状态
     *
     * @param taskId Mureka 任务 ID
     * @return 完整的任务状态 JSON
     */
    public JsonNode queryInstrumentalTask(String taskId) {
        return get("/v1/instrumental/query/" + taskId);
    }

    /**
     * 上传音频文件到 Mureka (用于 recognize)
     *
     * @param audioData 音频字节数据
     * @param filename  文件名 (如 "music_123.mp3")
     * @return 上传文件 ID (upload_audio_id)
     */
    public String uploadAudio(byte[] audioData, String filename) {
        try {
            String boundary = "----MurekaUpload" + System.currentTimeMillis();
            byte[] body = buildMultipartBody(boundary, audioData, filename, "audio");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/files/upload"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("[MurekaClient] Upload audio → {}", response.statusCode());

            if (response.statusCode() != 200) {
                log.error("[MurekaClient] Upload 失败: {} {}", response.statusCode(), response.body());
                throw new RuntimeException("Mureka 文件上传失败: " + response.statusCode());
            }

            JsonNode resp = objectMapper.readTree(response.body());
            return resp.get("id").asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Mureka 文件上传异常", e);
        }
    }

    /**
     * 识别歌曲 → 获取带时间戳的歌词
     *
     * @param uploadAudioId 上传文件 ID
     * @return { duration, lyrics_sections: [{start, end, text}] }
     */
    public JsonNode recognizeSong(String uploadAudioId) {
        var body = Map.of("upload_audio_id", uploadAudioId);
        return post("/v1/song/recognize", body);
    }

    // =========== Internal HTTP ===========

    /**
     * 构建 multipart/form-data 请求体
     */
    private byte[] buildMultipartBody(String boundary, byte[] fileData, String filename, String purpose) {
        try {
            var baos = new java.io.ByteArrayOutputStream();
            var writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(baos, "UTF-8"), true);

            // purpose 字段
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"purpose\"\r\n\r\n");
            writer.append(purpose).append("\r\n");
            writer.flush();

            // file 字段
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename)
                    .append("\"\r\n");
            writer.append("Content-Type: audio/mpeg\r\n\r\n");
            writer.flush();
            baos.write(fileData);
            writer.append("\r\n");

            // 终止
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("构建 multipart 失败", e);
        }
    }

    private JsonNode post(String path, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("[MurekaClient] POST {} → {}", path, response.statusCode());

            if (response.statusCode() != 200) {
                log.error("[MurekaClient] API 错误: {} {}", response.statusCode(), response.body());
                throw new RuntimeException("Mureka API 调用失败: " + response.statusCode());
            }

            return objectMapper.readTree(response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Mureka API 调用异常", e);
        }
    }

    private JsonNode get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("[MurekaClient] GET {} → {}", path, response.statusCode());

            if (response.statusCode() != 200) {
                log.error("[MurekaClient] API 错误: {} {}", response.statusCode(), response.body());
                throw new RuntimeException("Mureka API 查询失败: " + response.statusCode());
            }

            return objectMapper.readTree(response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Mureka API 查询异常", e);
        }
    }
}
