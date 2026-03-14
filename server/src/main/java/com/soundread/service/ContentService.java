package com.soundread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soundread.adapter.LlmAdapter;
import com.soundread.mapper.StudioProjectMapper;
import com.soundread.mapper.WorkMapper;
import com.soundread.model.entity.StudioProject;
import com.soundread.model.entity.Work;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 内容发现服务 (含 AI 推荐与审核)
 *
 * <h3>两级缓存策略（游客高频读）</h3>
 * <ul>
 * <li>L1：前端 sessionStorage（120 秒，纯客户端）</li>
 * <li>L2：Redis JSON 缓存（5 分钟），省去高频游客请求打穿 MySQL</li>
 * </ul>
 *
 * <p>
 * 缓存 Key 规范：
 * 
 * <pre>
 *   discover:works:{category}:{sort}:p{page}:s{size}
 *   discover:banners
 * </pre>
 * 
 * 只缓存前 {@code MAX_CACHED_PAGES} 页，避免缓存膨胀。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final WorkMapper workMapper;
    private final StudioProjectMapper projectMapper;
    private final LlmAdapter llmAdapter;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 仅缓存前 N 页（游客基本只看第一屏） */
    private static final int MAX_CACHED_PAGES = 2;
    /** Redis 缓存 TTL：5 分钟 */
    private static final long CACHE_TTL_MINUTES = 5;

    private static final String KEY_BANNERS = "discover:banners";
    private static final String KEY_PREFIX_WORKS = "discover:works:";

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

    // ======================== 公共 API ========================

    /**
     * 获取作品列表（分页 + 分类 + 排序），前 {@code MAX_CACHED_PAGES} 页走 Redis 缓存
     */
    public Page<Work> getWorks(String category, String sort, int page, int size) {
        // 只缓存前几页热门数据
        if (page <= MAX_CACHED_PAGES) {
            String cacheKey = buildWorksKey(category, sort, page, size);
            Page<Work> cached = readCache(cacheKey, new TypeReference<Page<Work>>() {
            });
            if (cached != null) {
                return cached;
            }
            Page<Work> result = queryWorks(category, sort, page, size);
            writeCache(cacheKey, result);
            return result;
        }
        return queryWorks(category, sort, page, size);
    }

    /**
     * 增加播放计数（Redis 缓冲，定期同步到 DB）
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
            return false;
        } else {
            redisTemplate.opsForValue().set(key, "1");
            return true;
        }
    }

    /**
     * AI 内容审核 — 发布前自动检测
     */
    public boolean moderateWork(String title, String content) {
        return llmAdapter.moderateContent(title + "\n" + content);
    }

    /**
     * 获取轮播图（热门作品），走 Redis 缓存
     */
    public List<Work> getBanners() {
        List<Work> cached = readCache(KEY_BANNERS, new TypeReference<List<Work>>() {
        });
        if (cached != null) {
            return cached;
        }
        List<Work> result = workMapper.selectList(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getStatus, "published")
                        .orderByDesc(Work::getPlayCount)
                        .last("LIMIT 5"));
        writeCache(KEY_BANNERS, result);
        return result;
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

        // 内容变更，清除发现页缓存
        evictDiscoverCache();
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

        // 内容变更，清除发现页缓存
        evictDiscoverCache();
        log.info("作品重新上架: workId={}, userId={}", workId, userId);
    }

    // ======================== 缓存工具 ========================

    /** 构建作品列表缓存 Key */
    private String buildWorksKey(String category, String sort, int page, int size) {
        String cat = (category == null || category.isBlank()) ? "all" : category;
        String srt = (sort == null || sort.isBlank()) ? "hot" : sort;
        return KEY_PREFIX_WORKS + cat + ":" + srt + ":p" + page + ":s" + size;
    }

    /** 从 Redis 读缓存，反序列化失败时静默降级 */
    private <T> T readCache(String key, TypeReference<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, type);
            }
        } catch (Exception e) {
            log.warn("[DiscoverCache] 读取缓存失败，降级查 DB: key={}, err={}", key, e.getMessage());
        }
        return null;
    }

    /** 序列化写入 Redis，写入失败不影响主流程 */
    private void writeCache(String key, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[DiscoverCache] 写入缓存失败: key={}, err={}", key, e.getMessage());
        }
    }

    /**
     * 清除所有发现页 Redis 缓存
     * 在作品发布/下架/上架时调用，保证内容一致性
     */
    private void evictDiscoverCache() {
        try {
            // 清 banners
            redisTemplate.delete(KEY_BANNERS);
            // 清 works 前 N 页（各分类 × 各排序 × 前 MAX_CACHED_PAGES 页）
            String[] categories = { "all", "audio", "novel", "podcast", "music" };
            String[] sorts = { "hot", "new", "latest" };
            for (String cat : categories) {
                for (String srt : sorts) {
                    for (int p = 1; p <= MAX_CACHED_PAGES; p++) {
                        redisTemplate.delete(KEY_PREFIX_WORKS + cat + ":" + srt + ":p" + p + ":s10");
                        redisTemplate.delete(KEY_PREFIX_WORKS + cat + ":" + srt + ":p" + p + ":s20");
                    }
                }
            }
            log.info("[DiscoverCache] 已清除发现页缓存");
        } catch (Exception e) {
            log.warn("[DiscoverCache] 清除缓存失败（不影响功能）: {}", e.getMessage());
        }
    }

    // ======================== 内部查询 ========================

    private Page<Work> queryWorks(String category, String sort, int page, int size) {
        LambdaQueryWrapper<Work> query = new LambdaQueryWrapper<Work>()
                .eq(Work::getStatus, "published");

        if (category != null && !"all".equals(category)) {
            query.eq(Work::getContentType, category);
        }

        switch (sort != null ? sort : "hot") {
            case "new", "latest" -> query.orderByDesc(Work::getCreatedAt);
            default -> query.orderByDesc(Work::getPlayCount, Work::getCreatedAt);
        }

        return workMapper.selectPage(new Page<>(page, size), query);
    }
}
