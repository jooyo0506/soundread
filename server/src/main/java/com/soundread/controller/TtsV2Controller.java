package com.soundread.controller;

import com.soundread.common.RequireFeature;
import com.soundread.common.Result;
import com.soundread.common.exception.BusinessException;
import com.soundread.controller.ttsv2.AiEmotionRequest;
import com.soundread.controller.ttsv2.AiSceneRequest;
import com.soundread.controller.ttsv2.TtsV2Request;
import com.soundread.controller.ttsv2.TtsV2Response;
import com.soundread.controller.ttsv2.TtsV2Service;
import com.soundread.model.entity.UserCreation;
import com.soundread.model.dto.SmartToneResult;
import com.soundread.model.entity.User;
import com.soundread.service.AiScriptService;
import com.soundread.service.AuthService;
import com.soundread.service.CreationService;
import com.soundread.service.QuotaService;
import com.soundread.service.StorageQuotaService;
import com.soundread.service.ToneMatchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TTS v2.0 语音合成接口
 *
 * <p>
 * 对接 Seed-TTS 2.0 协议，支持情感标签控制，提供比 v1 更强的感情表现力。
 * </p>
 *
 * <p>
 * 接口列表：
 * </p>
 * <ul>
 * <li>POST /synthesize - 情感语音合成</li>
 * <li>POST /enhance-tags - AI 情感标签增强</li>
 * <li>POST /generate-scene - AI 场景台本生成</li>
 * <li>POST /analyze-mood - 情感分析</li>
 * <li>POST /smart-tone - 内容智能音色匹配</li>
 * </ul>
 *
 * @author SoundRead
 */
@Slf4j
@RestController
@RequestMapping("/api/tts/v2")
@RequiredArgsConstructor
public class TtsV2Controller {

    private final TtsV2Service ttsV2Service;
    private final AuthService authService;
    private final QuotaService quotaService;
    private final AiScriptService aiScriptService;
    private final CreationService creationService;
    private final StorageQuotaService storageQuotaService;
    private final ToneMatchService toneMatchService;

    /**
     * 情感语音合成（TTS 2.0 核心接口）
     *
     * <p>
     * 通过 WebSocket 与 Seed-TTS 2.0 通信，支持情感标签控制。
     * 文本中的 [#XXX] 标签会被解析为对应的情感/停顿/强调指令。
     * </p>
     *
     * @param request     合成请求（文本、音色等）
     * @param httpRequest HTTP 上下文
     * @return 音频 URL
     */
    @PostMapping("/synthesize")
    public Result<TtsV2Response> synthesize(
            @RequestBody TtsV2Request request,
            HttpServletRequest httpRequest) {
        log.info("TtsV2 合成请求: {}", request);
        try {
            User user = authService.getCurrentUser();
            // 检查并扣除 TTS 2.0 配额
            int textLen = request.getText() != null ? request.getText().length() : 300;
            quotaService.checkAndDeductTextV2Quota(user, textLen);
            // 检查存储配额
            storageQuotaService.checkStorageQuota(user, textLen * 200L);
            TtsV2Response response = ttsV2Service.synthesize(request);

            // 保存创作记录
            try {
                UserCreation creation = new UserCreation();
                creation.setUserId(user.getId());
                creation.setType("emotion");
                creation.setInputText(request.getText() != null
                        ? request.getText().substring(0, Math.min(request.getText().length(), 500))
                        : "");
                creation.setVoiceId(request.getVoiceType());
                creation.setAudioUrl(response.getAudioUrl());
                creation.setAudioDuration(
                        Math.max(1, (int) ((request.getText() != null ? request.getText().length() : 0) / 4.5)));
                creationService.save(creation);
            } catch (Exception ex) {
                log.warn("TtsV2 保存创作记录失败: {}", ex.getMessage());
            }

            return Result.ok(response);
        } catch (Exception e) {
            log.error("TTSv2 合成服务端失败", e);
            return Result.fail("合成失败：" + e.getMessage());
        }
    }

    /**
     * AI 情感标签增强
     *
     * <p>
     * 对用户输入的原始文本进行 AI 处理，自动插入情感标签，提升语音表现力：
     * 1. [#停顿XXms] 停顿控制
     * 2. [#情感词] 情感渲染标签
     * </p>
     *
     * @param request 原始文本请求
     * @return 增强后的文本（含情感标签）
     */
    @RequireFeature("ai_script")
    @PostMapping("/enhance-tags")
    public Result<String> enhanceEmotionTags(@Valid @RequestBody AiEmotionRequest request) {
        try {
            User user = authService.getCurrentUser();
            // TTS 合成字数已在 synthesize 接口统一计量，此处仅做功能鲁检

            String enhancedText = aiScriptService.enhanceEmotionTags(user, request.getText());
            return Result.ok(enhancedText);
        } catch (Exception e) {
            log.error("情感标签增强失败", e);
            return Result.fail("增强失败：" + e.getMessage());
        }
    }

    /**
     * AI 场景台本生成
     *
     * <p>
     * 根据 mode 参数选择不同的 AI Agent 策略：
     * <ul>
     * <li>mode="quick" - 调用 QuickDubbingAgent</li>
     * <li>mode="director" - 调用 DirectorScriptAgent</li>
     * </ul>
     * </p>
     *
     * @param request 包含 instruction/wordCount/theme/context/mode 字段
     * @return AI 生成的台本文本
     */
    @RequireFeature("ai_script")
    @PostMapping("/generate-scene")
    public Result<String> generateScene(@Valid @RequestBody AiSceneRequest request) {
        try {
            User user = authService.getCurrentUser();
            // TTS 合成字数已在 synthesize 接口统一计量，此处仅做功能鲁检

            String sceneScript = aiScriptService.generateSceneScript(
                    user,
                    request.getInstruction(),
                    request.getWordCount(),
                    request.getTheme(),
                    request.getContext(),
                    request.getMode());
            return Result.ok(sceneScript);

        } catch (BusinessException e) {
            log.error("场景台本生成业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("场景台本生成失败", e);
            return Result.fail("生成失败：" + e.getMessage());
        }
    }

    /**
     * AI 情感分析（对话模式辅助）
     *
     * <p>
     * 分析输入文本的情感倾向，为 Director Mode 的 UX 决策提供数据支撑。
     * </p>
     *
     * @param body 包含 context 字段（待分析文本）
     * @return JSON 格式的情感分析结果
     */
    @RequireFeature("ai_script")
    @PostMapping("/analyze-mood")
    public Result<String> analyzeMood(@RequestBody java.util.Map<String, String> body) {
        try {
            User user = authService.getCurrentUser();
            String context = body.getOrDefault("context", "");
            if (context.isBlank()) {
                return Result.fail("分析文本不能为空");
            }
            String result = aiScriptService.analyzeMood(user, context);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("情感分析失败", e);
            return Result.fail("分析失败：" + e.getMessage());
        }
    }

    /**
     * 内容智能音色匹配（AI 推荐 + LLM 角色分析）
     *
     * <p>
     * 根据内容语义和 ai_prompt_role 模块策略，推荐最适合的音色。
     * </p>
     *
     * @param body 包含 content（文本内容）和 moduleType（模块类型）字段
     * @return SmartToneResult（推荐音色列表 + 理由）
     */
    @RequireFeature("ai_script")
    @PostMapping("/smart-tone")
    public Result<SmartToneResult> smartToneMatch(@RequestBody java.util.Map<String, String> body) {
        try {
            User user = authService.getCurrentUser();
            String content = body.getOrDefault("content", "");
            String moduleType = body.getOrDefault("moduleType", "general");
            if (content.isBlank()) {
                return Result.fail("匹配内容不能为空");
            }
            SmartToneResult result = toneMatchService.smartMatch(user, content, moduleType);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("智能音色匹配失败", e);
            return Result.fail("匹配失败：" + e.getMessage());
        }
    }
}
