package com.soundread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soundread.model.entity.UserCreation;
import com.soundread.mapper.UserCreationMapper;
import com.soundread.model.entity.User;
import com.soundread.model.entity.Work;
import com.soundread.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户创作记录管理服务
 *
 * <p>
 * 职责:
 * 1. 保存合成记录并更新存储计量
 * 2. 分页查询个人创作列表
 * 3. 发布 creation → work 以及下架联动
 * 4. 删除时释放存储空间
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreationService {

    private final UserCreationMapper creationMapper;
    private final WorkMapper workMapper;
    private final StorageQuotaService storageQuotaService;
    private final TierPolicyService tierPolicyService;

    /**
     * 保存一条合成记录并同步更新存储计量
     *
     * <p>
     * 由 TtsController / TtsV2Service / TtsDramaService / PodcastHandler 调用。
     * 1) 插入 user_creation 2) 更新 user_storage 存储量
     * </p>
     *
     * @param creation 必须包含 userId/type/audioUrl/fileSize 字段
     * @return 插入后的实体（含自增 ID）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserCreation save(UserCreation creation) {
        if (creation.getTitle() == null || creation.getTitle().isBlank()) {
            creation.setTitle(generateDefaultTitle(creation.getType()));
        }

        // 1. 插入创作记录
        creationMapper.insert(creation);

        // 2. 同步更新 user_storage 存储计量
        long fileSize = creation.getFileSize() != null ? creation.getFileSize() : 0;
        if (fileSize > 0) {
            storageQuotaService.addStorage(creation.getUserId(), fileSize);
        }

        log.info("创作记录保存成功: id={} userId={} type={} size={}",
                creation.getId(), creation.getUserId(), creation.getType(), fileSize);
        return creation;
    }

    /**
     * 分页查询用户创作列表
     *
     * @param userId 用户 ID
     * @param type   类型过滤，null 表示不过滤
     * @param page   页码
     * @param size   每页数量
     * @return 分页结果
     */
    public IPage<UserCreation> listByUser(Long userId, String type, int page, int size) {
        LambdaQueryWrapper<UserCreation> wrapper = new LambdaQueryWrapper<UserCreation>()
                .eq(UserCreation::getUserId, userId)
                .eq(type != null && !type.isBlank(), UserCreation::getType, type)
                .orderByDesc(UserCreation::getCreatedAt);
        return creationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 发布创作到发现页
     *
     * <p>
     * 将 user_creation 发布为 work，支持 auto_publish（VIP 自动过审）。
     * </p>
     *
     * @param creationId 创作记录 ID
     * @param title      作品标题
     * @param category   作品分类
     * @param user       当前用户
     * @return 发布后的 work ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long creationId, String title, String category, User user) {
        UserCreation creation = creationMapper.selectById(creationId);
        if (creation == null || !creation.getUserId().equals(user.getId())) {
            throw new com.soundread.common.exception.BusinessException("创作记录不存在");
        }
        if (creation.getIsPublished() != null && creation.getIsPublished() == 1) {
            throw new com.soundread.common.exception.BusinessException("该记录已发布");
        }

        // 构建 work 记录
        Work work = new Work();
        work.setUserId(user.getId());
        work.setTitle(title != null ? title : creation.getTitle());
        work.setCategory(category != null ? category : "latest");
        work.setAudioUrl(creation.getAudioUrl());
        work.setAudioDuration(creation.getAudioDuration() != null ? creation.getAudioDuration() : 0);
        work.setStatus("published");

        // VIP 用户支持 auto_publish 直接过审
        boolean autoPublish = tierPolicyService.hasFeature(user.getTierCode(), "auto_publish");

        workMapper.insert(work);

        // 更新 creation 发布状态
        creation.setIsPublished(1);
        creation.setWorkId(work.getId());
        creationMapper.updateById(creation);

        log.info("创作发布成功: creationId={} workId={} autoPublish={}", creationId, work.getId(), autoPublish);
        return work.getId();
    }

    /**
     * 重命名创作记录，同步更新关联 work 的标题
     *
     * @param creationId 创作记录 ID
     * @param newTitle   新标题
     * @param userId     当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void rename(Long creationId, String newTitle, Long userId) {
        UserCreation creation = creationMapper.selectById(creationId);
        if (creation == null || !creation.getUserId().equals(userId)) {
            throw new com.soundread.common.exception.BusinessException("记录不存在");
        }
        creation.setTitle(newTitle.trim());
        creationMapper.updateById(creation);

        // 如果已发布则同步更新 work 标题
        if (creation.getWorkId() != null) {
            Work work = workMapper.selectById(creation.getWorkId());
            if (work != null) {
                work.setTitle(newTitle.trim());
                workMapper.updateById(work);
            }
        }
        log.info("创作重命名: creationId={} title={}", creationId, newTitle);
    }

    /**
     * 下架已发布的 work，清除 creation 的发布状态
     *
     * @param creationId 创作记录 ID
     * @param userId     当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long creationId, Long userId) {
        UserCreation creation = creationMapper.selectById(creationId);
        if (creation == null || !creation.getUserId().equals(userId)) {
            throw new com.soundread.common.exception.BusinessException("记录不存在");
        }
        if (creation.getIsPublished() == null || creation.getIsPublished() != 1) {
            throw new com.soundread.common.exception.BusinessException("该记录尚未发布");
        }

        // 删除关联 work
        if (creation.getWorkId() != null) {
            workMapper.deleteById(creation.getWorkId());
        }

        // 清除 creation 发布状态
        creation.setIsPublished(0);
        creation.setWorkId(null);
        creationMapper.updateById(creation);

        log.info("创作下架: creationId={}", creationId);
    }

    /**
     * 删除创作记录并释放存储空间
     *
     * @param creationId 创作记录 ID
     * @param userId     当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long creationId, Long userId) {
        UserCreation creation = creationMapper.selectById(creationId);
        if (creation == null || !creation.getUserId().equals(userId)) {
            throw new com.soundread.common.exception.BusinessException("记录不存在");
        }

        // 若已发布，先删除关联的 work
        if (creation.getIsPublished() != null && creation.getIsPublished() == 1 && creation.getWorkId() != null) {
            workMapper.deleteById(creation.getWorkId());
        }

        // 删除创作记录
        creationMapper.deleteById(creationId);

        // 释放存储计量
        if (creation.getFileSize() != null && creation.getFileSize() > 0) {
            storageQuotaService.releaseStorage(userId, creation.getFileSize());
        }

        log.info("创作删除: creationId={} fileSize={}", creationId, creation.getFileSize());
    }

    /**
     * 统计用户总创作数量
     */
    public long countByUser(Long userId) {
        return creationMapper.selectCount(
                new LambdaQueryWrapper<UserCreation>().eq(UserCreation::getUserId, userId));
    }

    /**
     * 统计用户总文件大小 (字节)
     */
    public long sumFileSizeByUser(Long userId) {
        var wrapper = new LambdaQueryWrapper<UserCreation>()
                .eq(UserCreation::getUserId, userId)
                .select(UserCreation::getFileSize);
        var list = creationMapper.selectList(wrapper);
        return list.stream()
                .mapToLong(c -> c.getFileSize() != null ? c.getFileSize() : 0)
                .sum();
    }

    /**
     * 根据创作类型生成默认标题
     */
    private String generateDefaultTitle(String type) {
        String prefix;
        switch (type) {
            case "tts":
                prefix = "语音合成";
                break;
            case "emotion":
                prefix = "情感配音";
                break;
            case "drama":
                prefix = "剧本创作";
                break;
            case "podcast":
                prefix = "AI 投稿";
                break;
            case "novel":
                prefix = "有声小说";
                break;
            default:
                prefix = "创作";
                break;
        }
        return prefix + " " + LocalDateTime.now().toLocalDate();
    }
}
