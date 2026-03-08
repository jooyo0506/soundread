package com.soundread.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soundread.adapter.R2StorageAdapter;
import com.soundread.model.entity.UserCreation;
import com.soundread.mapper.UserCreationMapper;
import com.soundread.mapper.UserMapper;
import com.soundread.mapper.UserStorageMapper;
import com.soundread.model.entity.SysTierPolicy;
import com.soundread.model.entity.User;
import com.soundread.service.TierPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据生命周期清理定时任务
 *
 * <p>
 * 根据 sys_tier_policy.quota_limits.data_retention_days 配置，自动清理过期的创作记录：
 * <ul>
 * <li>免费用户：默认保留 30 天</li>
 * <li>VIP 月度：保留 180 天</li>
 * <li>VIP 年度：保留 365 天</li>
 * <li>终身 VIP：永久保留 (-1)</li>
 * </ul>
 * </p>
 *
 * <p>
 * 清理流程：
 * 1. 按用户分组查询过期且未发布的创作记录
 * 2. 删除 R2 上的音频文件
 * 3. 释放 user_storage.used_bytes 存储计量
 * 4. 删除 user_creation 记录
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionJob {

    private final UserCreationMapper userCreationMapper;
    private final UserStorageMapper userStorageMapper;
    private final UserMapper userMapper;
    private final TierPolicyService tierPolicyService;
    private final R2StorageAdapter r2StorageAdapter;

    /**
     * 每天凌晨 3:00 执行清理
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredCreations() {
        log.info("[DataRetentionJob] 开始清理过期创作记录");

        int totalCleaned = 0;

        // 按用户分组查询所有有创作记录的用户
        List<UserCreation> allCreations = userCreationMapper.selectList(
                new LambdaQueryWrapper<UserCreation>()
                        .select(UserCreation::getUserId)
                        .groupBy(UserCreation::getUserId));

        for (UserCreation uc : allCreations) {
            Long userId = uc.getUserId();
            int retentionDays = getRetentionDays(userId);

            // -1 表示永久保留，跳过
            if (retentionDays < 0) {
                continue;
            }

            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            // 查询过期且未发布的创作记录
            List<UserCreation> expired = userCreationMapper.selectList(
                    new LambdaQueryWrapper<UserCreation>()
                            .eq(UserCreation::getUserId, userId)
                            .lt(UserCreation::getCreatedAt, cutoff)
                            .eq(UserCreation::getIsPublished, 0));

            if (expired.isEmpty()) {
                continue;
            }

            long releasedBytes = 0;
            int releasedFiles = 0;

            for (UserCreation creation : expired) {
                // 删除 R2 上的音频文件
                try {
                    if (creation.getAudioUrl() != null && !creation.getAudioUrl().isEmpty()) {
                        r2StorageAdapter.deleteByUrl(creation.getAudioUrl());
                    }
                } catch (Exception e) {
                    log.warn("[DataRetentionJob] R2 文件删除失败: url={}, error={}",
                            creation.getAudioUrl(), e.getMessage());
                }

                // 累计释放的存储空间
                if (creation.getFileSize() != null) {
                    releasedBytes += creation.getFileSize();
                }
                releasedFiles++;

                // 删除创作记录
                userCreationMapper.deleteById(creation.getId());
                totalCleaned++;
            }

            // 更新用户存储计量（delta 为负值表示释放）
            if (releasedBytes > 0) {
                userStorageMapper.updateStorageDelta(userId, -releasedBytes, -releasedFiles);
                log.info("[DataRetentionJob] 用户 {} 清理 {} 条记录, 释放 {} 字节",
                        userId, expired.size(), releasedBytes);
            }
        }

        log.info("[DataRetentionJob] 清理完成, 共删除 {} 条过期记录", totalCleaned);
    }

    /**
     * 获取用户的数据保留天数
     *
     * <p>
     * 根据 userId 查询 user.tierCode，通过 TierPolicyService 获取
     * quotaLimits.dataRetentionDays
     * </p>
     */
    private int getRetentionDays(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            String tierCode = (user != null && user.getTierCode() != null) ? user.getTierCode() : "user";
            SysTierPolicy policy = tierPolicyService.getByTierCode(tierCode);
            if (policy != null && policy.getQuotaLimits() != null) {
                return policy.getQuotaLimits().getDataRetentionDays();
            }
        } catch (Exception e) {
            log.warn("[DataRetentionJob] 获取用户 {} 保留策略失败, 使用默认30天", userId);
        }
        return 30; // 默认 30 天
    }
}
