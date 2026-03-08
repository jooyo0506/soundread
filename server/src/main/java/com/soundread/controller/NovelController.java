package com.soundread.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.soundread.common.Result;
import com.soundread.model.entity.NovelChapter;
import com.soundread.model.entity.NovelProject;
import com.soundread.model.entity.NovelSegment;
import com.soundread.model.entity.User;
import com.soundread.service.AuthService;
import com.soundread.service.NovelPipelineService;
import com.soundread.service.NovelService;
import com.soundread.service.TierPolicyService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 有声书项目 Controller
 *
 * <p>
 * 提供有声书全生命周期的 REST 接口：
 * <ul>
 * <li>创建项目、查询详情和列表、删除项目</li>
 * <li>触发 AI Pipeline（章节切割 → 情感标注 → TTS 合成）</li>
 * <li>查询章节列表、段落列表</li>
 * <li>实时轮询处理进度</li>
 * </ul>
 * </p>
 *
 * @author SoundRead
 */
@RestController
@RequestMapping("/api/novel")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;
    private final NovelPipelineService pipelineService;
    private final AuthService authService;
    private final TierPolicyService tierPolicyService;

    /**
     * 创建有声书项目
     *
     * <p>
     * 创建前会校验用户等级是否拥有 {@code ai_novel} 功能权限；
     * 创建成功后项目处于 {@code draft} 状态，需手动触发 Pipeline 才开始处理。
     * </p>
     *
     * @param req 创建请求（标题、音色 ID、原始文本）
     * @return 新建的 NovelProject 实体
     */
    @PostMapping("/create")
    public Result<NovelProject> create(@RequestBody CreateRequest req) {
        User user = authService.getCurrentUser();
        // 校验功能权限，非授权等级返回失败
        if (!tierPolicyService.hasFeature(user.getTierCode(), "ai_novel")) {
            return Result.fail("当前会员等级不支持 AI 有声书功能");
        }
        NovelProject project = novelService.create(user, req.getTitle(), req.getVoiceId(), req.getRawText());
        return Result.ok(project);
    }

    /**
     * 启动 AI 有声书处理 Pipeline（异步执行）
     *
     * <p>
     * 仅允许状态为 {@code draft}（初稿）或 {@code failed}（失败重试）的项目启动。
     * Pipeline 将依次完成：章节切割 → 情感标注 → TTS 合成。
     * </p>
     *
     * @param id  项目 ID
     * @param req 启动请求（包含原始小说文本）
     */
    @PostMapping("/{id}/start")
    public Result<Void> startPipeline(@PathVariable Long id, @RequestBody StartRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        NovelProject project = novelService.getDetail(id, userId);
        if (!"draft".equals(project.getStatus()) && !"failed".equals(project.getStatus())) {
            return Result.fail("项目当前状态不允许重新启动：" + project.getStatus());
        }
        pipelineService.startPipeline(id, req.getRawText());
        return Result.ok(null);
    }

    /**
     * 查询当前用户的所有有声书项目列表（按创建时间降序）
     */
    @GetMapping("/list")
    public Result<List<NovelProject>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(novelService.listByUser(userId));
    }

    /**
     * 查询单个有声书项目详情（含权限校验）
     *
     * @param id 项目 ID
     */
    @GetMapping("/{id}")
    public Result<NovelProject> detail(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(novelService.getDetail(id, userId));
    }

    /**
     * 查询项目下所有章节列表（按章节序号升序）
     *
     * @param id 项目 ID
     */
    @GetMapping("/{id}/chapters")
    public Result<List<NovelChapter>> chapters(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        novelService.getDetail(id, userId); // 权限校验
        return Result.ok(novelService.listChapters(id));
    }

    /**
     * 查询指定章节下所有语义段落（按段落序号升序）
     *
     * @param chapterId 章节 ID
     */
    @GetMapping("/chapter/{chapterId}/segments")
    public Result<List<NovelSegment>> segments(@PathVariable Long chapterId) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 校验章节归属权限
        novelService.checkChapterOwnership(chapterId, userId);
        return Result.ok(novelService.listSegments(chapterId));
    }

    /**
     * 查询项目的实时处理进度
     *
     * <p>
     * 前端可通过轮询此接口获取 Pipeline 当前阶段和百分比进度，
     * 直到 status 变为 {@code completed} 或 {@code failed} 停止轮询。
     * </p>
     *
     * @param id 项目 ID
     * @return ProgressInfo 包含状态、进度百分比、总章节数、音频 URL
     */
    @GetMapping("/{id}/progress")
    public Result<ProgressInfo> progress(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        NovelProject project = novelService.getDetail(id, userId);
        ProgressInfo info = new ProgressInfo();
        info.setStatus(project.getStatus());
        info.setProgress(project.getProgress());
        info.setTotalChapters(project.getTotalChapters());
        info.setAudioUrl(project.getAudioUrl());
        return Result.ok(info);
    }

    /**
     * 删除有声书项目（含章节、段落级联删除及存储配额释放）
     *
     * @param id 项目 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        novelService.delete(id, userId);
        return Result.ok(null);
    }

    // ==================== 请求 / 响应 DTO ====================

    @Data
    public static class CreateRequest {
        /** 项目标题 */
        private String title;
        /** 音色 ID（对应 sys_voice.voice_id） */
        private String voiceId;
        /** 原始小说文本（AI 将对此文本进行章节切割和情感标注） */
        private String rawText;
    }

    @Data
    public static class StartRequest {
        /** 原始小说文本（用于 Pipeline 处理，可与创建时相同） */
        private String rawText;
    }

    @Data
    public static class ProgressInfo {
        /**
         * 当前处理状态（draft / analyzing / annotating / synthesizing / completed / failed）
         */
        private String status;
        /** 处理进度百分比（0~100，-1 表示失败） */
        private int progress;
        /** 已切割的总章节数 */
        private int totalChapters;
        /** 合成完成后的音频访问 URL（处理中为 null） */
        private String audioUrl;
    }
}
