package com.soundread.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.soundread.adapter.R2StorageAdapter;
import com.soundread.agent.creative.MusicLyricAgent;
import com.soundread.config.ai.LlmRouter;
import com.soundread.model.entity.MusicTask;
import com.soundread.model.entity.Work;
import com.soundread.mapper.MusicTaskMapper;
import com.soundread.mapper.WorkMapper;
import com.soundread.sdk.mureka.MurekaClient;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 音乐服务
 *
 * <p>
 * 职责：
 * 1. 提交生成任务 (歌曲/纯音乐) → Mureka API
 * 2. AI 歌词生成 → DeepSeek (via LlmRouter)
 * 3. 定时轮询 Mureka 任务状态
 * 4. 管理用户任务列表
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicTaskMapper musicTaskMapper;
    private final WorkMapper workMapper;
    private final MurekaClient murekaClient;
    private final LlmRouter llmRouter;
    private final MusicLyricAgent lyricAgent;
    private final AuthService authService;
    private final R2StorageAdapter r2StorageAdapter;

    /**
     * 提交音乐生成任务
     *
     * @param taskType song / instrumental
     * @param prompt   风格提示词
     * @param lyrics   歌词 (song 类型必填)
     * @param model    模型选择
     * @return 任务信息
     */
    public MusicTask submitGenerate(String taskType, String prompt, String lyrics, String model) {
        Long userId = StpUtil.getLoginIdAsLong();

        MusicTask task = new MusicTask();
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setPrompt(prompt);
        task.setLyrics(lyrics);
        task.setModel(model);
        task.setStatus("processing");

        // 生成标题
        String title = prompt != null && prompt.length() > 20
                ? prompt.substring(0, 20) + "..."
                : (prompt != null ? prompt : "AI 音乐");
        task.setTitle(title);

        try {
            String murekaTaskId;
            if ("song".equals(taskType)) {
                murekaTaskId = murekaClient.generateSong(lyrics, prompt, model);
            } else {
                murekaTaskId = murekaClient.generateInstrumental(prompt, model);
            }
            task.setMurekaTaskId(murekaTaskId);
        } catch (Exception e) {
            log.error("[MusicService] 提交 Mureka 任务失败", e);
            task.setStatus("failed");
            task.setErrorMsg(e.getMessage());
        }

        task.setCreatedAt(LocalDateTime.now());
        musicTaskMapper.insert(task);
        return task;
    }

    /**
     * AI 生成歌词 — 使用 DeepSeek (via LlmRouter)
     *
     * <p>
     * 根据用户输入的风格标签 (rock / pop / r&b / jazz / classical / electronic
     * / hip-hop / folk / Chinese / slow / energetic) 生成专业歌词。
     * 输出格式对标 Mureka 歌词引擎：[Intro] [Verse] [Chorus] [Bridge] [Outro]
     * </p>
     *
     * @param prompt 风格描述（如 "rock" 或 "r&b, slow, passionate"）
     * @return { lyrics }
     */
    public Map<String, String> generateLyrics(String prompt) {
        var user = authService.getCurrentUser();
        var chatModel = llmRouter.getChatModelWithFallback(user);

        String systemPrompt = lyricAgent.buildLyricsSystemPrompt();
        String userPrompt = lyricAgent.buildLyricsUserPrompt(prompt);

        var result = chatModel.generate(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt));

        String lyrics = result.content().text().trim();
        log.info("[MusicService] DeepSeek 歌词生成完成, prompt={}, length={}", prompt, lyrics.length());

        return Map.of("lyrics", lyrics);
    }

    /**
     * 查询任务状态
     */
    public MusicTask getTask(Long taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        MusicTask task = musicTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new RuntimeException("任务不存在");
        }
        return task;
    }

    /**
     * 获取用户的任务列表
     */
    public List<MusicTask> listMyTasks() {
        Long userId = StpUtil.getLoginIdAsLong();
        return musicTaskMapper.selectList(
                new LambdaQueryWrapper<MusicTask>()
                        .eq(MusicTask::getUserId, userId)
                        .eq(MusicTask::getDeleted, 0)
                        .orderByDesc(MusicTask::getCreatedAt));
    }

    /**
     * 删除任务
     */
    public void deleteTask(Long taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        MusicTask task = musicTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new RuntimeException("任务不存在");
        }
        musicTaskMapper.deleteById(taskId);
    }

    /**
     * 发布音乐到灵感大厅（创建 Work 记录）
     */
    public Map<String, Object> publishTask(Long taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        MusicTask task = musicTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new RuntimeException("任务不存在");
        }
        if (!"succeeded".equals(task.getStatus())) {
            throw new RuntimeException("任务尚未完成，无法发布");
        }
        if (task.getResultUrl() == null || task.getResultUrl().isBlank()) {
            throw new RuntimeException("音频文件不存在");
        }

        // 查重：同一个 MusicTask 不重复发布
        Work existing = workMapper.selectOne(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getCreationId, taskId)
                        .eq(Work::getSourceType, "music")
                        .eq(Work::getUserId, userId)
                        .ne(Work::getStatus, "unpublished")
                        .last("LIMIT 1"));
        if (existing != null) {
            throw new RuntimeException("该作品已发布过");
        }

        Work work = new Work();
        work.setUserId(userId);
        work.setCreationId(taskId);
        work.setTitle(task.getTitle() != null ? task.getTitle()
                : (task.getPrompt() != null ? task.getPrompt().substring(0, Math.min(30, task.getPrompt().length()))
                        : "AI 音乐"));
        work.setDescription(task.getPrompt());
        work.setCategory("music");
        work.setContentType("music");
        work.setSourceType("music");
        work.setAudioUrl(task.getResultUrl());
        work.setAudioDuration(task.getDuration() != null ? task.getDuration() / 1000 : null);
        work.setStatus("published");
        work.setPlayCount(0);
        work.setLikeCount(0);
        work.setShareCount(0);
        work.setCommentCount(0);
        workMapper.insert(work);

        log.info("音乐发布成功: taskId={}, workId={}", taskId, work.getId());

        return Map.of(
                "workId", work.getId(),
                "message", "发布成功 🎉");
    }

    /**
     * 重命名音乐
     */
    public void renameTask(Long taskId, String newTitle) {
        Long userId = StpUtil.getLoginIdAsLong();
        MusicTask task = musicTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new RuntimeException("任务不存在");
        }
        if (newTitle == null || newTitle.isBlank()) {
            throw new RuntimeException("标题不能为空");
        }
        task.setTitle(newTitle.trim());
        musicTaskMapper.updateById(task);

        // 同步更新已发布的 Work 标题
        Work work = workMapper.selectOne(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getCreationId, taskId)
                        .eq(Work::getSourceType, "music")
                        .eq(Work::getUserId, userId)
                        .last("LIMIT 1"));
        if (work != null) {
            work.setTitle(newTitle.trim());
            workMapper.updateById(work);
        }

        log.info("音乐重命名: taskId={}, newTitle={}", taskId, newTitle);
    }

    /**
     * 下架音乐（从灵感大厅移除）
     */
    public void unpublishTask(Long taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        MusicTask task = musicTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new RuntimeException("任务不存在");
        }

        Work work = workMapper.selectOne(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getCreationId, taskId)
                        .eq(Work::getSourceType, "music")
                        .eq(Work::getUserId, userId)
                        .eq(Work::getStatus, "published")
                        .last("LIMIT 1"));
        if (work == null) {
            throw new RuntimeException("未找到已上架的作品");
        }

        work.setStatus("unpublished");
        workMapper.updateById(work);

        log.info("音乐下架: taskId={}, workId={}", taskId, work.getId());
    }

    /**
     * 定时轮询 Mureka 任务状态 (每 5 秒)
     *
     * <p>
     * 当任务成功时，下载 Mureka 临时音频 URL 并上传到 R2 持久化存储。
     * </p>
     */
    @Scheduled(fixedDelay = 5000)
    public void pollMurekaTasks() {
        List<MusicTask> pendingTasks = musicTaskMapper.selectList(
                new LambdaQueryWrapper<MusicTask>()
                        .in(MusicTask::getStatus, "processing", "streaming")
                        .isNotNull(MusicTask::getMurekaTaskId));

        for (MusicTask task : pendingTasks) {
            try {
                JsonNode result;
                if ("song".equals(task.getTaskType())) {
                    result = murekaClient.querySongTask(task.getMurekaTaskId());
                } else {
                    result = murekaClient.queryInstrumentalTask(task.getMurekaTaskId());
                }

                String status = result.get("status").asText();

                if ("streaming".equals(status)) {
                    // ★ 流式阶段 — 获取 HLS 流地址，前端可以边生成边听
                    JsonNode choices = result.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode first = choices.get(0);
                        if (first.has("stream_url") && !first.get("stream_url").isNull()) {
                            String streamUrl = first.get("stream_url").asText();
                            if (task.getStreamUrl() == null || task.getStreamUrl().isBlank()) {
                                task.setStreamUrl(streamUrl);
                                task.setStatus("streaming");
                                log.info("[MusicService] 进入流式播放: taskId={}, streamUrl={}", task.getId(), streamUrl);
                            }
                        }
                    }
                } else if ("succeeded".equals(status)) {
                    // 获取音频 URL
                    JsonNode choices = result.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode first = choices.get(0);
                        String tempUrl = first.get("url").asText();
                        if (first.has("duration")) {
                            task.setDuration(first.get("duration").asInt());
                        }

                        // 下载 Mureka 临时音频 → 持久化到 R2
                        byte[] audioData = downloadAudio(tempUrl);
                        if (audioData != null) {
                            String filename = "music_" + task.getId() + "_" + System.currentTimeMillis() + ".mp3";
                            String r2Url = r2StorageAdapter.uploadMusic(audioData, filename);
                            task.setResultUrl(r2Url);
                            log.info("[MusicService] 音频已持久化到 R2: {} ({} bytes)", r2Url, audioData.length);

                            // ★ 异步获取歌词时间戳 (不阻塞主轮询)
                            if ("song".equals(task.getTaskType())) {
                                final Long taskId = task.getId();
                                final byte[] audio = audioData;
                                new Thread(() -> recognizeAndSaveTimings(taskId, audio), "lyrics-recognize-" + taskId)
                                        .start();
                            }
                        } else {
                            task.setResultUrl(tempUrl); // 降级：使用原始 URL
                        }
                    }
                    task.setStatus("succeeded");
                    task.setFinishedAt(LocalDateTime.now());
                    if (result.has("model")) {
                        task.setModel(result.get("model").asText());
                    }
                    log.info("[MusicService] 任务完成: id={}, url={}", task.getId(), task.getResultUrl());

                } else if ("failed".equals(status) || "timeouted".equals(status) || "cancelled".equals(status)) {
                    task.setStatus("failed");
                    task.setErrorMsg(result.has("failed_reason")
                            ? result.get("failed_reason").asText()
                            : status);
                    task.setFinishedAt(LocalDateTime.now());
                    log.warn("[MusicService] 任务失败: id={}, reason={}", task.getId(), task.getErrorMsg());
                }
                // preparing / queued / running / reviewing → 继续轮询

                musicTaskMapper.updateById(task);

            } catch (Exception e) {
                log.error("[MusicService] 轮询任务异常: taskId={}", task.getId(), e);
            }
        }
    }

    /**
     * ★ 异步识别歌词时间戳
     *
     * <p>
     * 流程: 上传音频到 Mureka → 调用 recognize → 保存时间戳 JSON
     * 失败时静默降级（前端使用启发式算法兜底）
     * </p>
     */
    private void recognizeAndSaveTimings(Long taskId, byte[] audioData) {
        try {
            log.info("[MusicService] ★ 开始识别歌词时间戳: taskId={}", taskId);

            // 1. 上传音频到 Mureka
            String uploadId = murekaClient.uploadAudio(audioData, "music_" + taskId + ".mp3");
            log.info("[MusicService] 音频已上传 Mureka: uploadId={}", uploadId);

            // 2. 调用 recognize 获取时间戳
            JsonNode recognizeResult = murekaClient.recognizeSong(uploadId);
            JsonNode sections = recognizeResult.get("lyrics_sections");

            if (sections != null && sections.isArray() && sections.size() > 0) {
                // 3. 保存时间戳到数据库
                String timingsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(sections);
                MusicTask update = new MusicTask();
                update.setId(taskId);
                update.setLyricTimings(timingsJson);
                musicTaskMapper.updateById(update);

                log.info("[MusicService] ★ 歌词时间戳已保存: taskId={}, sections={}", taskId, sections.size());
            } else {
                log.warn("[MusicService] recognize 返回空 lyrics_sections: taskId={}", taskId);
            }
        } catch (Exception e) {
            // 静默失败 — 前端降级为启发式算法
            log.error("[MusicService] 歌词时间戳识别失败 (前端将降级): taskId={}", taskId, e);
        }
    }

    /**
     * 下载外部音频
     *
     * @param sourceUrl Mureka 临时音频 URL
     * @return 音频字节数据，失败返回 null
     */
    private byte[] downloadAudio(String sourceUrl) {
        try {
            var httpClient = java.net.http.HttpClient.newHttpClient();
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(sourceUrl))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .GET()
                    .build();

            var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                log.error("[MusicService] 下载 Mureka 音频失败: status={}, url={}", response.statusCode(), sourceUrl);
                return null;
            }

            log.info("[MusicService] 音频已下载: {} bytes", response.body().length);
            return response.body();

        } catch (Exception e) {
            log.error("[MusicService] 下载音频异常", e);
            return null;
        }
    }
}
