package com.soundread.controller;

import com.soundread.common.RequireFeature;
import com.soundread.common.Result;
import com.soundread.model.dto.TtsDto;
import com.soundread.model.entity.User;
import com.soundread.adapter.R2StorageAdapter;
import com.soundread.adapter.Tts1Adapter;
import com.soundread.sdk.tts.model.TtsResponse;
import com.soundread.model.entity.UserCreation;
import com.soundread.service.AiScriptService;
import com.soundread.service.AuthService;
import com.soundread.service.CreationService;
import com.soundread.service.QuotaService;
import com.soundread.service.StorageQuotaService;
import com.soundread.service.VoiceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * TTS 1.0 语音合成接口
 *
 * <p>
 * 提供 TTS 1.0 引擎的三个能力出口：
 * <ul>
 * <li>短文本同步合成（支持速度/音量/音调调整）</li>
 * <li>AI 台本生成（SSE 流式响应）</li>
 * <li>长文本异步任务提交 + 查询</li>
 * </ul>
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    /** SSE 超时时间 2 分钟 */
    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final AuthService authService;
    private final QuotaService quotaService;
    private final AiScriptService aiScriptService;
    private final VoiceService voiceService;
    private final Tts1Adapter tts1Adapter;
    private final R2StorageAdapter r2StorageAdapter;
    private final CreationService creationService;
    private final StorageQuotaService storageQuotaService;

    /**
     * 短文本同步合成（TTS 1.0）
     */
    @PostMapping("/short")
    public Result<TtsDto.SynthesizeResponse> shortTextSynthesize(
            @Valid @RequestBody TtsDto.ShortTextRequest req) {
        User user = authService.getCurrentUser();
        quotaService.checkAndDeductTextQuota(user, req.getText().length());
        // 粗略估算：每字符约 200 字节 MP3
        storageQuotaService.checkStorageQuota(user, req.getText().length() * 200L);

        // 检查音色权限（supported_engines 字段决定）
        // tts-2.0 是 VIP 专属，tts-1.0 所有用户可用
        String engine = voiceService.detectVoiceEngine(req.getVoiceId());
        if (!voiceService.checkUserVoicePermission(user.getId(), req.getVoiceId(), engine)) {
            return Result.fail("无权使用该音色，请升级VIP或购买");
        }

        // 调用 TTS 1.0 合成
        float speed = req.getSpeed() != null ? req.getSpeed() : 1.0f;
        float volume = req.getVolume() != null ? req.getVolume() : 1.0f;
        float pitch = req.getPitch() != null ? req.getPitch() : 1.0f;

        byte[] audioData = tts1Adapter.synthesize(req.getText(), req.getVoiceId(), speed, volume, pitch);

        // 上传到 R2 存储
        String audioUrl = r2StorageAdapter.uploadAudio(audioData, "tts_" + System.currentTimeMillis() + ".mp3");

        TtsDto.SynthesizeResponse resp = new TtsDto.SynthesizeResponse();
        resp.setAudioUrl(audioUrl);
        // 估算时长：约 4-5 字/秒，取 3 字/秒保守估算
        int duration = Math.max(1, req.getText().length() / 4);
        resp.setDuration(duration);

        // 保存创作记录
        try {
            UserCreation creation = new UserCreation();
            creation.setUserId(user.getId());
            creation.setType("tts");
            creation.setInputText(req.getText());
            creation.setVoiceId(req.getVoiceId());
            creation.setAudioUrl(audioUrl);
            creation.setAudioDuration(duration);
            creation.setFileSize((long) audioData.length);
            creationService.save(creation);
        } catch (Exception e) {
            log.warn("保存创作记录失败: {}", e.getMessage());
        }

        return Result.ok(resp);
    }

    /**
     * 音色试听（免费，不扣配额）
     *
     * <p>
     * 仅用于音色库试听场景，固定合成一段短文本。
     * 不扣费、不检查权限、不保存创作记录。
     * </p>
     */
    @PostMapping("/preview")
    public Result<TtsDto.SynthesizeResponse> previewVoice(@RequestBody TtsDto.ShortTextRequest req) {
        String text = req.getText();
        if (text == null || text.isBlank()) {
            text = "大家好，这是我的声音，希望你会喜欢。";
        }
        // 限制试听文本长度，防止滥用
        if (text.length() > 50) {
            text = text.substring(0, 50);
        }

        try {
            byte[] audioData = tts1Adapter.synthesize(text, req.getVoiceId(), 1.0f, 1.0f, 1.0f);
            String audioUrl = r2StorageAdapter.uploadAudio(audioData, "preview_" + req.getVoiceId() + ".mp3");

            TtsDto.SynthesizeResponse resp = new TtsDto.SynthesizeResponse();
            resp.setAudioUrl(audioUrl);
            resp.setDuration(Math.max(1, text.length() / 4));
            return Result.ok(resp);
        } catch (Exception e) {
            log.warn("[TTS Preview] 试听合成失败 voiceId={}: {}", req.getVoiceId(), e.getMessage());
            return Result.fail("试听合成失败，请稍后重试");
        }
    }

    /**
     * AI 台本生成（SSE 流式响应）
     */
    @RequireFeature("ai_script")
    @PostMapping(value = "/ai-script", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateAiScript(@Valid @RequestBody TtsDto.AiScriptRequest req) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        try {
            User user = authService.getCurrentUser();

            // 检查并扣除 AI 台本额度
            quotaService.checkAndDeductAiScriptQuota(user);

            aiScriptService.generateScriptStream(user, req.getPrompt())
                    .onNext(chunk -> {
                        try {
                            emitter.send(SseEmitter.event().data(chunk));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .onComplete(response -> {
                        emitter.complete();
                    })
                    .onError(error -> {
                        log.error("AI 台本生成失败", error);
                        try {
                            emitter.send(SseEmitter.event().data("\n\n[AI 生成失败，请重试]"));
                        } catch (Exception sendErr) {
                            log.warn("SSE 错误消息发送失败: {}", sendErr.getMessage());
                        }
                        emitter.completeWithError(error);
                    })
                    .start();
        } catch (Exception e) {
            log.error("AI 台本接口异常", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 长文本异步任务提交
     */
    @PostMapping("/long-text")
    public Result<TtsDto.TaskResponse> submitLongText(
            @Valid @RequestBody TtsDto.LongTextRequest req) {
        User user = authService.getCurrentUser();
        quotaService.checkAndDeductTextQuota(user, req.getText().length());

        // 检查音色权限
        if (!voiceService.checkUserVoicePermission(user.getId(), req.getVoiceId(), "tts-1.0")) {
            return Result.fail("无权使用该音色");
        }

        // 提交到 Tts1Adapter (内部为异步任务)
        boolean useEmotion = req.getUseEmotion() != null && req.getUseEmotion();
        String taskId = tts1Adapter.createLongTextTask(req.getText(), req.getVoiceId(), useEmotion);

        TtsDto.TaskResponse resp = new TtsDto.TaskResponse();
        resp.setTaskId(taskId);
        resp.setStatus("pending");
        return Result.ok(resp);
    }

    /**
     * 长文本异步任务查询
     */
    @GetMapping("/long-text/{taskId}")
    public Result<TtsDto.TaskResponse> queryLongTextTask(
            @PathVariable String taskId,
            @RequestParam(required = false, defaultValue = "false") boolean useEmotion) {

        // 轮询任务状态
        TtsResponse sdkResp = tts1Adapter.queryLongTextTask(taskId, useEmotion);

        TtsDto.TaskResponse resp = new TtsDto.TaskResponse();
        resp.setTaskId(taskId);

        // 映射 SDK status 到前端 status
        if (sdkResp.getTaskStatus() == TtsResponse.TaskStatus.SUCCESS) {
            resp.setStatus("success");
            resp.setAudioUrl(sdkResp.getAudioUrl());
            resp.setProgress(100);
        } else if (sdkResp.getTaskStatus() == TtsResponse.TaskStatus.FAILED) {
            resp.setStatus("failed");
        } else {
            resp.setStatus("processing");
            resp.setProgress(50); // TODO: 可接入实时句子级进度
        }

        return Result.ok(resp);
    }
}
