package com.soundread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soundread.common.exception.BusinessException;
import com.soundread.mapper.UserStorageMapper;
import com.soundread.mapper.WorkMapper;
import com.soundread.model.entity.Work;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 运营作品管理服务
 *
 * <p>
 * 提供面向运营后台的作品审核、精选推荐、统计看板等能力。
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminWorkService {

    private final WorkMapper workMapper;
    private final UserStorageMapper userStorageMapper;

    /**
     * 分页查询作品列表（支持多维筛选）
     *
     * @param reviewStatus 审核状态筛选 (pending/approved/rejected，null=全部)
     * @param sourceType   来源类型筛选 (tts/emotion/novel 等，null=全部)
     * @param keyword      标题关键词搜索
     * @param page         页码
     * @param size         每页条数
     * @return 分页结果
     */
    public Page<Work> listWorks(String reviewStatus, String sourceType, String keyword,
            int page, int size) {
        LambdaQueryWrapper<Work> query = new LambdaQueryWrapper<>();

        if (reviewStatus != null && !reviewStatus.isEmpty()) {
            query.eq(Work::getReviewStatus, reviewStatus);
        }
        if (sourceType != null && !sourceType.isEmpty()) {
            query.eq(Work::getSourceType, sourceType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.like(Work::getTitle, keyword);
        }

        query.orderByDesc(Work::getCreatedAt);
        return workMapper.selectPage(new Page<>(page, size), query);
    }

    /**
     * 审核作品
     *
     * @param workId     作品ID
     * @param action     操作 (approve / reject)
     * @param reviewNote 审核备注
     * @param reviewerId 审核人ID
     */
    public void reviewWork(Long workId, String action, String reviewNote, Long reviewerId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }

        Work update = new Work();
        update.setId(workId);
        update.setReviewNote(reviewNote);
        update.setReviewedAt(LocalDateTime.now());
        update.setReviewedBy(reviewerId);

        switch (action) {
            case "approve" -> {
                update.setReviewStatus("approved");
                update.setStatus("published");
            }
            case "reject" -> {
                update.setReviewStatus("rejected");
                update.setStatus("rejected");
            }
            default -> throw new BusinessException("无效的审核操作: " + action);
        }

        workMapper.updateById(update);
        log.info("作品审核: workId={}, action={}, reviewer={}", workId, action, reviewerId);
    }

    /**
     * 切换精选状态
     */
    public void toggleFeatured(Long workId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }

        Work update = new Work();
        update.setId(workId);
        update.setIsFeatured(work.getIsFeatured() == null || work.getIsFeatured() == 0 ? 1 : 0);
        workMapper.updateById(update);

        log.info("精选切换: workId={}, featured={}", workId, update.getIsFeatured());
    }

    /**
     * 删除作品（逻辑删除）
     */
    public void deleteWork(Long workId) {
        workMapper.deleteById(workId);
        log.info("作品删除: workId={}", workId);
    }

    /**
     * 上架/下架作品
     */
    public void togglePublish(Long workId, String status) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        if ("published".equals(status) && !"approved".equals(work.getReviewStatus())) {
            throw new BusinessException("只有审核通过的作品才能上架");
        }

        Work update = new Work();
        update.setId(workId);
        update.setStatus(status);
        workMapper.updateById(update);

        log.info("作品{}：workId={}", "published".equals(status) ? "上架" : "下架", workId);
    }

    /**
     * 运营看板统计
     *
     * @return 各维度统计数据
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>(8);

        // 总作品数
        stats.put("totalWorks", workMapper.selectCount(null));

        // 待审核数
        stats.put("pendingCount", workMapper.selectCount(
                new LambdaQueryWrapper<Work>().eq(Work::getReviewStatus, "pending")));

        // 已通过数
        stats.put("approvedCount", workMapper.selectCount(
                new LambdaQueryWrapper<Work>().eq(Work::getReviewStatus, "approved")));

        // 已拒绝数
        stats.put("rejectedCount", workMapper.selectCount(
                new LambdaQueryWrapper<Work>().eq(Work::getReviewStatus, "rejected")));

        // 精选数
        stats.put("featuredCount", workMapper.selectCount(
                new LambdaQueryWrapper<Work>().eq(Work::getIsFeatured, 1)));

        return stats;
    }

    /**
     * 手动更新热度分（后续由 HeatScoreJob 定时调用）
     *
     * <p>
     * 公式：播放×1 + 点赞×3 + 分享×5 + 评论×2 + 精选加权50
     * </p>
     */
    public void refreshHeatScore(Long workId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            return;
        }

        int play = work.getPlayCount() != null ? work.getPlayCount() : 0;
        int like = work.getLikeCount() != null ? work.getLikeCount() : 0;
        int share = work.getShareCount() != null ? work.getShareCount() : 0;
        int comment = work.getCommentCount() != null ? work.getCommentCount() : 0;
        int featured = (work.getIsFeatured() != null && work.getIsFeatured() == 1) ? 50 : 0;

        BigDecimal score = BigDecimal.valueOf(play + like * 3L + share * 5L + comment * 2L + featured);

        Work update = new Work();
        update.setId(workId);
        update.setHeatScore(score);
        workMapper.updateById(update);
    }
}
