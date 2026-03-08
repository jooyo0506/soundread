package com.soundread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soundread.adapter.LlmAdapter;
import com.soundread.mapper.StudioProjectMapper;
import com.soundread.mapper.WorkMapper;
import com.soundread.model.entity.StudioProject;
import com.soundread.model.entity.Work;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内容发现服务 (含 AI 推荐与审核)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final WorkMapper workMapper;
    private final StudioProjectMapper projectMapper;
    private final LlmAdapter llmAdapter;
    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void fixLegacyContentTypes() {
        // 修复旧数据：将 category=podcast 但 contentType=audio 的作品修正为 contentType=podcast
        int updated = workMapper.update(null, new LambdaUpdateWrapper<Work>()
                .eq(Work::getCategory, "podcast")
                .eq(Work::getContentType, "audio")
                .set(Work::getContentType, "podcast"));
        if (updated > 0) {
            log.info("修复了 {} 条播客作品的 contentType", updated);
        }
    }

    /**
     * 获取作品列表 (分页 + 分类 + 排序)
     */
    public Page<Work> getWorks(String category, String sort, int page, int size) {
        LambdaQueryWrapper<Work> query = new LambdaQueryWrapper<Work>()
                .eq(Work::getStatus, "published");

        // 按 contentType 过滤（"all" 不过滤）
        if (category != null && !"all".equals(category)) {
            query.eq(Work::getContentType, category);
        }

        // 排序
        switch (sort != null ? sort : "hot") {
            case "new", "latest" -> query.orderByDesc(Work::getCreatedAt);
            default -> query.orderByDesc(Work::getPlayCount, Work::getCreatedAt);
        }

        return workMapper.selectPage(new Page<>(page, size), query);
    }

    /**
     * 增加播放计数 (Redis 缓冲，定期同步到 DB)
     */
    public void incrementPlayCount(Long workId) {
        redisTemplate.opsForValue().increment("work:play:" + workId);
    }

    /**
     * 点赞/取消
     */
    public boolean toggleLike(Long userId, Long workId) {
        String key = "work:like:" + workId + ":" + userId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            redisTemplate.delete(key);
            return false; // 取消点赞
        } else {
            redisTemplate.opsForValue().set(key, "1");
            return true; // 点赞
        }
    }

    /**
     * AI 内容审核 — 发布前自动检测
     */
    public boolean moderateWork(String title, String content) {
        return llmAdapter.moderateContent(title + "\n" + content);
    }

    /**
     * 获取轮播图 (简化: 返回热门作品)
     */
    public List<Work> getBanners() {
        return workMapper.selectList(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getStatus, "published")
                        .orderByDesc(Work::getPlayCount)
                        .last("LIMIT 5"));
    }

    /**
     * 作者下架自己的作品
     */
    public void unpublishWork(Long workId, Long userId) {
        Work work = workMapper.selectById(workId);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此作品");
        }
        if (!"published".equals(work.getStatus())) {
            throw new RuntimeException("作品当前不在线");
        }

        // 下架作品
        work.setStatus("unpublished");
        workMapper.updateById(work);

        // 同步关联的工作台项目状态
        if (work.getSourceProjectId() != null) {
            StudioProject project = projectMapper.selectById(work.getSourceProjectId());
            if (project != null && "completed".equals(project.getStatus())) {
                project.setStatus("editing");
                projectMapper.updateById(project);
            }
        }

        log.info("作品下架: workId={}, userId={}", workId, userId);
    }

    /**
     * 作者重新上架作品
     */
    public void republishWork(Long workId, Long userId) {
        Work work = workMapper.selectById(workId);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此作品");
        }
        if (!"unpublished".equals(work.getStatus())) {
            throw new RuntimeException("作品当前不可上架");
        }

        work.setStatus("published");
        workMapper.updateById(work);

        // 同步关联的工作台项目状态
        if (work.getSourceProjectId() != null) {
            StudioProject project = projectMapper.selectById(work.getSourceProjectId());
            if (project != null) {
                project.setStatus("completed");
                projectMapper.updateById(project);
            }
        }

        log.info("作品重新上架: workId={}, userId={}", workId, userId);
    }
}
