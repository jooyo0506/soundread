package com.soundread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soundread.common.exception.BusinessException;
import com.soundread.model.entity.NovelChapter;
import com.soundread.model.entity.NovelProject;
import com.soundread.model.entity.NovelSegment;
import com.soundread.mapper.NovelChapterMapper;
import com.soundread.mapper.NovelProjectMapper;
import com.soundread.mapper.NovelSegmentMapper;
import com.soundread.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ?CRUD
 *
 * <p>
 * ?
 * Pipeline ?{@link NovelPipelineService} ?
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelProjectMapper projectMapper;
    private final NovelChapterMapper chapterMapper;
    private final NovelSegmentMapper segmentMapper;
    private final StorageQuotaService storageQuotaService;
    private final QuotaService quotaService;

    /**
     * ?
     *
     * @param user
     * @param title
     * @param voiceId D
     * @param rawText
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public NovelProject create(User user, String title, String voiceId, String rawText) {
        //
        long currentCount = projectMapper.selectCount(
                new LambdaQueryWrapper<NovelProject>().eq(NovelProject::getUserId, user.getId()));
        storageQuotaService.checkProjectQuota(user, (int) currentCount);

        // ?
        quotaService.checkAndDeductNovelCharsQuota(user, rawText.length());

        NovelProject project = new NovelProject();
        project.setUserId(user.getId());
        project.setTitle(title);
        project.setVoiceId(voiceId);
        project.setTotalChars(rawText.length());
        project.setStatus("draft");
        project.setProgress(0);
        projectMapper.insert(project);

        log.info("[NovelService] 创建项目成功: projectId={} title={} textLen={}", project.getId(), title, rawText.length());
        return project;
    }

    /**
     * 查询指定用户的所有有声书项目，按创建时间降序
     */
    public List<NovelProject> listByUser(Long userId) {
        return projectMapper.selectList(
                new LambdaQueryWrapper<NovelProject>()
                        .eq(NovelProject::getUserId, userId)
                        .orderByDesc(NovelProject::getCreatedAt));
    }

    /**
     * 查询项目详情，并校验当前用户权限
     *
     * @param projectId 项目 ID
     * @param userId    当前用户 ID
     * @return NovelProject 实体
     * @throws BusinessException 项目不存在或无权访问
     */
    public NovelProject getDetail(Long projectId, Long userId) {
        NovelProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            throw new BusinessException("项目不存在");
        }
        return project;
    }

    /**
     * 查询项目下所有章节，按章节序号升序排列
     *
     * @param projectId 项目 ID
     * @return 有序章节列表
     */
    public List<NovelChapter> listChapters(Long projectId) {
        return chapterMapper.selectList(
                new LambdaQueryWrapper<NovelChapter>()
                        .eq(NovelChapter::getProjectId, projectId)
                        .orderByAsc(NovelChapter::getChapterIndex));
    }

    /**
     * 查询章节下所有语义段落，按段落序号升序排列
     *
     * @param chapterId 章节 ID
     * @return 有序段落列表
     */
    public List<NovelSegment> listSegments(Long chapterId) {
        return segmentMapper.selectList(
                new LambdaQueryWrapper<NovelSegment>()
                        .eq(NovelSegment::getChapterId, chapterId)
                        .orderByAsc(NovelSegment::getSegmentIndex));
    }

    /**
     * 校验章节归属权限，确认属于指定用户
     *
     * @param chapterId 章节 ID
     * @param userId    当前用户 ID
     * @throws BusinessException 章节不存在或无权访问
     */
    public void checkChapterOwnership(Long chapterId, Long userId) {
        NovelChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        // 通过 getDetail 完成项目归属与权限验证
        getDetail(chapter.getProjectId(), userId);
    }

    /**
     * 删除有声书项目（含权限校验、章节、段落级联删除及存储释放）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long projectId, Long userId) {
        NovelProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            throw new BusinessException("项目不存在");
        }

        // 逐章节删除并统计待释放的存储字节数
        List<NovelChapter> chapters = listChapters(projectId);
        long totalReleaseBytes = 0;
        for (NovelChapter chapter : chapters) {
            List<NovelSegment> segments = listSegments(chapter.getId());
            for (NovelSegment seg : segments) {
                // 估算音频占用存储（逸计: 16KB/s MP3）并删除段落记录
                if (seg.getAudioDuration() != null && seg.getAudioDuration() > 0) {
                    totalReleaseBytes += seg.getAudioDuration() * 16000L; // 估算: 16KB/s MP3
                }
            }
            segmentMapper.delete(
                    new LambdaQueryWrapper<NovelSegment>().eq(NovelSegment::getChapterId, chapter.getId()));
        }
        chapterMapper.delete(
                new LambdaQueryWrapper<NovelChapter>().eq(NovelChapter::getProjectId, projectId));
        projectMapper.deleteById(projectId);

        if (totalReleaseBytes > 0) {
            storageQuotaService.releaseStorage(userId, totalReleaseBytes);
        }

        log.info("[NovelService] 项目删除完成: projectId={} 释放存储={}B", projectId, totalReleaseBytes);
    }

    /**
     * 更新项目的处理进度和状态
     *
     * @param projectId 项目 ID
     * @param progress  进度百分比（0~100）或 -1 表示失败
     * @param status    状态标识（analyzing / annotating / synthesizing / completed /
     *                  failed）
     */
    public void updateProgress(Long projectId, int progress, String status) {
        NovelProject update = new NovelProject();
        update.setId(projectId);
        update.setProgress(progress);
        update.setStatus(status);
        projectMapper.updateById(update);
    }
}
