package com.soundread.controller;

import com.soundread.common.RateLimit;
import com.soundread.common.Result;
import com.soundread.model.entity.CreativeTemplate;
import com.soundread.model.entity.StudioProject;
import com.soundread.model.entity.StudioSection;
import com.soundread.model.entity.User;
import com.soundread.service.AuthService;
import com.soundread.service.QuotaService;
import com.soundread.service.ScriptParser;
import com.soundread.service.StudioService;
import com.soundread.service.TierPolicyService;
import com.soundread.adapter.R2StorageAdapter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI 创作工作台 Controller
 *
 * <p>
 * 提供创作类型查询、项目管理、AI 内容生成（SSE 流式）、段落管理等接口。
 * 所有返回值使用 Result<T> 包装，与前端 request.js 拦截器对齐。
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@RestController
@RequestMapping("/api/studio")
@RequiredArgsConstructor
public class StudioController {

    private final StudioService studioService;
    private final AuthService authService;
    private final QuotaService quotaService;
    private final TierPolicyService tierPolicyService;

    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;

    // ========================
    // 1. 创作类型
    // ========================

    /**
     * 获取所有启用的创作类型
     */
    @GetMapping("/templates")
    public Result<List<CreativeTemplate>> listTemplates() {
        return Result.ok(studioService.listTemplates());
    }

    // ========================
    // 2. 项目管理
    // ========================

    /**
     * 创建新项目
     */
    @PostMapping("/projects")
    public Result<StudioProject> createProject(@RequestBody CreateProjectRequest req) {
        return Result.ok(studioService.createProject(req.getTypeCode(), req.getTitle(), req.getInspiration()));
    }

    /**
     * 获取我的项目列表
     */
    @GetMapping("/projects")
    public Result<List<StudioProject>> listProjects() {
        return Result.ok(studioService.listMyProjects());
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/projects/{id}")
    public Result<StudioProject> getProject(@PathVariable Long id) {
        return Result.ok(studioService.getProject(id));
    }

    /**
     * 更新项目信息 (标题/大纲/角色设定)
     */
    @PutMapping("/projects/{id}")
    public Result<Void> updateProject(@PathVariable Long id, @RequestBody UpdateProjectRequest req) {
        StudioProject project = studioService.getProject(id);
        if (project == null) {
            return Result.fail("项目不存在");
        }
        if (req.getTitle() != null)
            project.setTitle(req.getTitle());
        if (req.getInspiration() != null)
            project.setInspiration(req.getInspiration());
        if (req.getOutline() != null)
            project.setOutline(req.getOutline());
        if (req.getCharacters() != null)
            project.setCharacters(req.getCharacters());
        if (req.getStatus() != null)
            project.setStatus(req.getStatus());
        studioService.updateProject(project);
        return Result.ok();
    }

    // ========================
    // 3. AI Agent 端点
    // ========================

    /**
     * AI 灵感种子生成 — 根据类型生成 6 个灵感
     */
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    @GetMapping("/templates/{typeCode}/inspiration")
    public Result<String> generateInspiration(@PathVariable String typeCode) {
        try {
            String result = studioService.generateInspiration(typeCode);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * AI 大纲生成 — 根据项目灵感生成结构化大纲和角色
     */
    @RateLimit(maxRequests = 5, windowSeconds = 60)
    @PostMapping("/projects/{id}/outline")
    public Result<String> generateOutline(@PathVariable Long id,
            @RequestBody(required = false) OutlineRequest req) {
        try {
            String result;
            if (req != null && req.getStylePreference() != null) {
                // 结构化大纲生成（小说/广播剧的大纲流程）
                result = studioService.generateStructuredOutline(
                        id, req.getStylePreference(),
                        req.getTargetChapters() != null ? req.getTargetChapters() : 5);
            } else {
                // 原有大纲生成（兼容）
                result = studioService.generateOutline(id);
            }
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    // ========================
    // 4. AI 内容生成 (SSE)
    // ========================

    /**
     * AI 创作生成 — 流式输出
     *
     * <p>
     * 权限策略：
     * <ul>
     * <li>novel / drama → 由各自 @RequireFeature 注解控制</li>
     * <li>lecture / radio / ad / picture_book / news → 基础用户可用，不限功能开关</li>
     * </ul>
     * 此处不加全局 @RequireFeature，在 StudioService 内部按 typeCode 判断。
     * </p>
     */
    @RateLimit(maxRequests = 5, windowSeconds = 60)
    @PostMapping(value = "/projects/{id}/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateContent(@PathVariable Long id, @RequestBody GenerateRequest req) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        try {
            // 配额检查：每次 AI 创作生成均消耗一次工作台次数
            User user = authService.getCurrentUser();
            quotaService.checkAndDeductAiScriptQuota(user);

            if (req.getChapterIndex() != null) {
                // 结构化内容生成：后端构建用户消息
                studioService.generateContentFromOutline(id, req.getChapterIndex(),
                        req.getOutlinePlot(), req.getKeyEvents(),
                        req.getForeshadowing(), req.getUserExtra(), emitter);
            } else {
                // 原有内容生成（兼容）
                studioService.generateContent(id, req.getInput(), emitter);
            }
        } catch (Exception e) {
            log.error("创作生成失败: ", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /**
     * 广播剧一键生成 — 对话驱动模式（SSE 流式）
     * <p>
     * ⚠️ 不使用 @RequireFeature AOP：AOP 在 SSE 端点上抛出异常时，
     * Spring MVC 内容类型协商失败会导致返回空响应，前端无法感知错误。
     * 改为在方法内部检查，与 TtsController.generateAiScript 保持一致。
     * </p>
     */
    @RateLimit(maxRequests = 3, windowSeconds = 60, message = "广播剧生成过于频繁，请稍后再试")
    @PostMapping(value = "/projects/{id}/drama-generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateDrama(@PathVariable Long id, @RequestBody DramaGenerateRequest req) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        try {
            User user = authService.getCurrentUser();

            // 功能权限校验（内联检查，避免 AOP + SSE 的内容协商冲突）
            if (!tierPolicyService.hasFeature(user.getTierCode(), "ai_drama")) {
                log.info("[广播剧] 功能未开通: userId={}, tierCode={}", user.getId(), user.getTierCode());
                emitter.send(SseEmitter.event().data("[DRAMA_ERROR]当前套餐未开通广播剧功能，请升级会员"));
                emitter.complete();
                return emitter;
            }

            // 配额检查：广播剧属于工作台内容生成，消耗 ai_script 次数
            quotaService.checkAndDeductAiScriptQuota(user);

            studioService.generateDrama(id, req.getDialogueMode(), req.getGenre(),
                    req.getGenreTips(), req.getCharacters(), req.getInspiration(), emitter);
        } catch (Exception e) {
            log.error("广播剧生成失败: ", e);
            try {
                // 发送错误事件，让前端能正确展示失败原因而非显示"0字"
                String errMsg = e.getMessage() != null ? e.getMessage() : "生成失败，请重试";
                emitter.send(SseEmitter.event().data("[DRAMA_ERROR]" + errMsg));
            } catch (Exception ignored) {
            }
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /**
     * AI 改写段落 — 流式输出
     */
    @PostMapping(value = "/sections/{id}/rewrite", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter rewriteSection(@PathVariable Long id, @RequestBody RewriteRequest req) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        try {
            studioService.rewriteSection(id, req.getInstruction(), emitter);
        } catch (Exception e) {
            log.error("改写失败: ", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    // ========================
    // 5. 段落管理
    // ========================

    /**
     * 获取项目的所有段落
     */
    @GetMapping("/projects/{id}/sections")
    public Result<List<StudioSection>> listSections(@PathVariable Long id) {
        return Result.ok(studioService.listSections(id));
    }

    /**
     * 保存/更新段落（带归属校验）
     */
    @PostMapping("/sections")
    public Result<Long> saveSection(@RequestBody StudioSection section) {
        studioService.checkSectionOwnership(section);
        studioService.saveSection(section);
        return Result.ok(section.getId());
    }

    /**
     * 删除段落（带归属校验）
     */
    @DeleteMapping("/sections/{id}")
    public Result<Void> deleteSection(@PathVariable Long id) {
        // 先查出段落，校验其所属项目的归属
        StudioSection section = studioService.getSectionById(id);
        if (section == null) {
            return Result.fail("段落不存在");
        }
        studioService.checkSectionOwnership(section);
        studioService.deleteSection(id);
        return Result.ok();
    }

    /**
     * 删除整个项目（含所有段落，带归属校验）
     */
    @DeleteMapping("/projects/{id}")
    public Result<Void> deleteProject(@PathVariable Long id) {
        StudioProject project = studioService.getProject(id);
        if (project == null) {
            return Result.fail("项目不存在");
        }
        studioService.deleteProject(id);
        return Result.ok();
    }
    // ========================
    // 6. 发布到发现页
    // ========================

    /**
     * 发布创作项目到发现页（待审核）
     */
    @PostMapping("/projects/{id}/publish")
    public Result<?> publishProject(@PathVariable Long id) {
        try {
            var result = studioService.publishProject(id);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 下架作品（从发现页移除，项目回退为 editing 状态）
     */
    @PostMapping("/projects/{id}/unpublish")
    public Result<?> unpublishProject(@PathVariable Long id) {
        try {
            var result = studioService.unpublishProject(id);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    // ========================
    // 7. 音频拼接
    // ========================

    /**
     * 将多段 TTS 音频拼接为一个文件
     */
    @PostMapping("/concat-audio")
    public Result<String> concatAudio(@RequestBody ConcatAudioRequest req) {
        try {
            String mergedUrl = studioService.concatAudio(req.getAudioUrls());
            return Result.ok(mergedUrl);
        } catch (Exception e) {
            log.error("音频拼接失败: ", e);
            return Result.fail("音频拼接失败: " + e.getMessage());
        }
    }

    // ========================
    // 8. 剧本解析（配音辅助）
    // ========================

    /**
     * 解析广播剧剧本 — 提取角色+对白，用于分角色配音
     */
    @PostMapping("/parse-script")
    public Result<?> parseScript(@RequestBody ParseScriptRequest req) {
        StudioSection section = studioService.getSectionById(req.getSectionId());
        if (section == null)
            return Result.fail("段落不存在");
        studioService.checkSectionOwnership(section);
        var result = ScriptParser.parseDramaScript(section.getContent());
        return Result.ok(Map.of(
                "lines", result.lines(),
                "characters", result.characters()));
    }

    /**
     * 清理文本元数据 — 移除角色名、音效标注等，返回纯朗读文本
     */
    @PostMapping("/strip-for-tts")
    public Result<String> stripForTTS(@RequestBody StripForTTSRequest req) {
        return Result.ok(ScriptParser.stripForTTS(req.getContent()));
    }

    // ========================
    // 8. 音频上传（播客发布用）
    // ========================

    private final R2StorageAdapter r2StorageAdapter;

    /**
     * 上传音频文件到 R2 持久化存储（用于播客生成后保存）
     */
    @PostMapping("/upload-audio")
    public Result<Map<String, String>> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            String filename = "podcast_" + System.currentTimeMillis() + ".mp3";
            String url = r2StorageAdapter.uploadAudio(file.getBytes(), filename);
            return Result.ok(Map.of("audioUrl", url));
        } catch (Exception e) {
            log.error("音频上传失败: ", e);
            return Result.fail("音频上传失败: " + e.getMessage());
        }
    }

    @Data
    public static class CreateProjectRequest {
        private String typeCode;
        private String title;
        private String inspiration;
    }

    @Data
    public static class UpdateProjectRequest {
        private String title;
        private String inspiration;
        private String outline;
        private String characters;
        private String status;
    }

    @Data
    public static class GenerateRequest {
        private String input; // 原有：自由输入模式
        // 结构化模式（前端零 Prompt）
        private Integer chapterIndex; // 当前章/幕索引（0-based）
        private String outlinePlot; // 本章/幕细纲
        private String keyEvents; // 核心事件
        private String foreshadowing; // 伏笔
        private String userExtra; // 用户补充要求
    }

    @Data
    public static class OutlineRequest {
        private String stylePreference; // 题材偏好（palace/heroine 等）
        private Integer targetChapters; // 目标章/幕数
    }

    @Data
    public static class RewriteRequest {
        private String instruction;
    }

    @Data
    public static class ConcatAudioRequest {
        private List<String> audioUrls;
    }

    @Data
    public static class ParseScriptRequest {
        private Long sectionId;
    }

    @Data
    public static class StripForTTSRequest {
        private String content;
    }

    @Data
    public static class DramaGenerateRequest {
        private String dialogueMode; // "duo" / "trio" / "ensemble"
        private String genre; // 题材 ID
        private String genreTips; // 题材创作提示
        private String characters; // 角色 JSON
        private String inspiration; // 灵感描述
    }
}
