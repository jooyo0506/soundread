package com.speech.volcengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speech.protocol.EventType;
import com.speech.protocol.Message;
import com.speech.protocol.MsgType;
import com.speech.protocol.SpeechWebSocketClient;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Slf4j
public class Podcasts {
    private static final String ENDPOINT = "wss://openspeech.bytedance.com/api/v3/sami/podcasttts";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get resource ID
     *
     * @return Corresponding resource ID
     */
    public static String resourceId() {
        return "volc.service_type.10050";
    }

    public static void main(String[] args) throws Exception {
        // Configure parameters
        String appId = System.getProperty("appId", "");
        String accessToken = System.getProperty("accessToken", "");
        String text = System.getProperty("text", "");
        String encoding = System.getProperty("encoding", "mp3");
        String inputId = System.getProperty("input_id", "test_podcast");
        boolean useHeadMusic = Boolean.parseBoolean(System.getProperty("use_head_music", "true"));
        String inputUrl = System.getProperty("input_url", "");
        String promptText = System.getProperty("prompt_text", "");
        String nlpTexts = System.getProperty("nlp_texts", "");
        String speakerInfo = System.getProperty("speaker_info", "{\"random_order\":false}");
        boolean useTailMusic = Boolean.parseBoolean(System.getProperty("use_tail_music", "false"));
        boolean onlyNlpText = Boolean.parseBoolean(System.getProperty("only_nlp_text", "false"));
        boolean returnAudioUrl = Boolean.parseBoolean(System.getProperty("return_audio_url", "false"));
        int action = Integer.parseInt(System.getProperty("action", "0")); // 动作类型，默认0
        String endpoint = System.getProperty("endpoint", ENDPOINT); // 允许自定义endpoint
        boolean skipRoundAudioSave = Boolean.parseBoolean(System.getProperty("skip_round_audio_save", "false"));

        // Check if appId and accessToken are set, not null and not empty
        if (appId.isEmpty() || accessToken.isEmpty()) {
            log.error("Please set appId and accessToken system properties");
            System.exit(1);
        }
        // check action in sets [0, 3, 4]
        if (action != 0 && action != 3 && action != 4) {
            log.error("action must be in sets [0, 3, 4]");
            System.exit(1);
        }

        // Set request headers
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Api-App-Id", appId);
        headers.put("X-Api-App-Key", "aGjiRDfUWi");
        headers.put("X-Api-Access-Key", accessToken);
        headers.put("X-Api-Resource-Id", resourceId());
        headers.put("X-Api-Connect-Id", UUID.randomUUID().toString());

        boolean isPodcastRoundEnd = true;
        int lastRoundId = -1;
        String taskId = "";
        int retryNum = 5;

        SpeechWebSocketClient client = null;
        byte[] podcastAudio = new byte[0];

        try {
            while (retryNum > 0) {
                // Create WebSocket client
                client = new SpeechWebSocketClient(new URI(endpoint), headers);
                client.connectBlocking();

                // 解析 nlp_texts: 若不为空则转为 JSON 数组（List<Map>），否则为 null
                List<Map<String, Object>> nlpTextsList = null; // 修正类型为 List<Map>
                if (!nlpTexts.isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        // 使用 TypeReference 显式指定 List<Map> 类型（解析 JSON 数组）
                        List<Map<String, Object>> parsed = objectMapper.readValue(nlpTexts,
                                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                        nlpTextsList = parsed;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse nlp_texts: " + e.getMessage(), e);
                    }
                }
                Map<String, Object> speakerInfoMap = null;
                if (!speakerInfo.isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = objectMapper.readValue(speakerInfo, Map.class);
                        speakerInfoMap = parsed;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse speaker_info: " + e.getMessage(), e);
                    }
                }

                // Prepare request parameters
                Map<String, Object> request = new HashMap<>();
                Map<String, Object> reqParams = new HashMap<>();
                reqParams.put("input_id", inputId);
                reqParams.put("input_text", text);
                reqParams.put("prompt_text", promptText);
                reqParams.put("action", action);
                reqParams.put("use_head_music", useHeadMusic);
                reqParams.put("use_tail_music", useTailMusic);
                // Only add nlp_texts if it's not null
                if (nlpTextsList != null) {
                    reqParams.put("nlp_texts", nlpTextsList);
                }
                // Only add speaker_info if it's not null
                if (speakerInfoMap!= null) {
                    reqParams.put("speaker_info", speakerInfoMap);
                }

                reqParams.put("audio_config", Map.of(
                        "format", encoding,
                        "sample_rate", 24000,
                        "speech_rate", 0
                ));
                reqParams.put("input_info", Map.of(
                        "input_url", inputUrl,
                        "return_audio_url", returnAudioUrl,
                        "only_nlp_text", onlyNlpText
                ));
                // If connection breaks in the middle, need to retry
                if (!isPodcastRoundEnd) {
                    reqParams.put("retry_info", Map.of(
                            "retry_task_id", taskId,
                            "last_finished_round_id", lastRoundId
                    ));
                }
                request.put("req_params", reqParams);

                log.info("request: {}", objectMapper.writeValueAsString(request));

                // Start connection
                client.sendStartConnection();

                // Process each sentence
                boolean audioReceived = false;

                String sessionId = UUID.randomUUID().toString();
                if (taskId.isEmpty()) {
                    taskId = sessionId;
                }

                byte[] audio = new byte[0];

                // Start session
                client.sendStartSession(objectMapper.writeValueAsBytes(request.get("req_params")), sessionId);

                // End session
                client.sendFinishSession(sessionId);

                String voice = "";
                int round = -10000;

                // 判断是否存在 output 目录，如果不存在则创建
                File outputDir = new File("output");
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                // 整个播客的文本数据
                List<Object> podcastTexts = new ArrayList<>();


                // Receive response
                while (true) {
                    Message msg = client.receiveMessage();

                    if (msg.getType() == MsgType.AUDIO_ONLY_SERVER && msg.getEvent() == EventType.PODCAST_ROUND_RESPONSE) {
                        if (!audioReceived && audio.length > 0) {
                            audioReceived = true;
                        }
                        byte[] newAudio = new byte[audio.length + msg.getPayload().length];
                        System.arraycopy(audio, 0, newAudio, 0, audio.length);
                        System.arraycopy(msg.getPayload(), 0, newAudio, audio.length, msg.getPayload().length);
                        audio = newAudio;
                    } else if (msg.getType() == MsgType.ERROR) {
                        throw new RuntimeException("Server returned error: " + new String(msg.getPayload()));
                    }

                    if (msg.getType() == MsgType.FULL_SERVER_RESPONSE && msg.getEvent() == EventType.PODCAST_ROUND_START) {
                        // Podcast round starts
                        String jsonString = new String(msg.getPayload(), StandardCharsets.UTF_8);
                        // Convert JSON string to Map
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
                        // Get voice and round
                        voice = (String) map.get("speaker");
                        round = (int) map.get("round_id");
                        if (round == -1) {
                            voice = "head_music";
                        }
                        if (round == 9999) {
                            voice = "tail_music";
                        }
                        isPodcastRoundEnd = false;

                        // 提取并保存文本信息（对应Python的text过滤逻辑）
                        if (map.get("text") != null) { // 检查text字段是否存在
                            Map<String, Object> filteredPayload = new HashMap<>();
                            filteredPayload.put("text", map.get("text"));       // 提取text
                            filteredPayload.put("speaker", map.get("speaker")); // 提取speaker
                            podcastTexts.add(filteredPayload);                 // 添加到列表
                        }

                        log.info("Starting new round: {}", jsonString);
                    }

                    if (msg.getType() == MsgType.FULL_SERVER_RESPONSE && msg.getEvent() == EventType.PODCAST_ROUND_END) {
                        // Podcast round starts
                        String jsonString = new String(msg.getPayload(), StandardCharsets.UTF_8);
                        // Convert JSON string to Map
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
                        log.info("Ending new round: {}", map);
                        if (map.containsKey("is_error")) {
                            break;
                        }
                        isPodcastRoundEnd = true;
                        lastRoundId = round;
                        // Podcast round audio ends
                        if (audio.length > 0 && !skipRoundAudioSave) {
                            String fileName = String.format("output/%s_%d.%s", voice, round, encoding);
                            Files.write(new File(fileName).toPath(), audio);
                            log.info("Audio saved to file: {}", fileName);
                        }
                        // Merge final audio
                        byte[] newAudio = new byte[podcastAudio.length + audio.length];
                        System.arraycopy(podcastAudio, 0, newAudio, 0, podcastAudio.length);
                        System.arraycopy(audio, 0, newAudio, podcastAudio.length, audio.length);
                        podcastAudio = newAudio;
                        audio = new byte[0];
                        continue;
                    }

                    if (msg.getType() == MsgType.FULL_SERVER_RESPONSE && msg.getEvent() == EventType.PODCAST_END) {
                        // Podcast ends
                        String jsonString = new String(msg.getPayload(), StandardCharsets.UTF_8);
                        // Convert JSON string to Map
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
                        log.info("Ending podcast: {}", map);
                    }

                    if (msg.getEvent() == EventType.SESSION_FINISHED) {
                        break;
                    }
                }

                if (!audioReceived && !onlyNlpText) {
                    throw new RuntimeException("No audio data received");
                }

                client.sendFinishConnection();

                // All audio ends
                if (isPodcastRoundEnd) {
                    if (podcastAudio.length > 0) {
                        // Save final audio, 名称加上当前时间戳
                        String timestamp = String.valueOf(System.currentTimeMillis());
                        String fileName = String.format("output/%s_%s.%s", "podcast_final_", timestamp, encoding);
                        Files.write(new File(fileName).toPath(), podcastAudio);
                        log.info("Current podcast audio saved to file: {}", fileName);
                    }
                    if (onlyNlpText && !podcastTexts.isEmpty()) {
                        String fileName = String.format("output/%s.%s", "podcast_texts", "json");
                        // 将 podcastTexts 列表转换为 JSON 字符串
                        String jsonString = objectMapper.writeValueAsString(podcastTexts);
                        // 将 JSON 字符串写入文件
                        Files.writeString(new File(fileName).toPath(), jsonString);
                        log.info("Current podcast texts saved to file: {}", fileName);
                    }
                    break;
                } else {
                    retryNum -= 1;
                    log.info("Current podcast not finished, resuming from round {}", lastRoundId);
                    Thread.sleep(1000);
                }
            }
        } finally {
            // End connection
            if (client != null) {
                client.closeBlocking();
            }
        }
    }
} 