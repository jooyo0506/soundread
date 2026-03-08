package com.soundread.service;

import com.soundread.model.entity.UserStorage;
import com.soundread.mapper.UserStorageMapper;
import com.soundread.model.dto.TierPolicyDto;
import com.soundread.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户存储配额管理服务
 *
 * <p>
 * 职责:
 * 1. 检查存储空间是否充足
 * 2. 检查项目数量是否达标
 * 3. 增加/释放存储计量
 * 4. 优先读取 quota_override_mb 进行特殊配额覆盖
 * </p>
 *
 * @author SoundRead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQuotaService {

    private final UserStorageMapper userStorageMapper;
    private final TierPolicyService tierPolicyService;

    /** MB 到 字节 的换算比例 */
    private static final long MB_TO_BYTES = 1024L * 1024L;

    /**
     * 检查用户可用存储空间是否充足
     *
     * @param user         用户实体
     * @param newFileBytes 即将存入的文件大小(字节)
     * @throws com.soundread.common.exception.QuotaExceededException 如果配额不足则抛出异常
     */
    public void checkStorageQuota(User user, long newFileBytes) {
        long maxBytes = getMaxStorageBytes(user);
        if (maxBytes == -1) {
            return; // 无限制
        }

        long usedBytes = getUsedBytes(user.getId());
        if (usedBytes + newFileBytes > maxBytes) {
            long usedMb = usedBytes / MB_TO_BYTES;
            long maxMb = maxBytes / MB_TO_BYTES;
            throw new com.soundread.common.exception.QuotaExceededException(
                    String.format("存储空间不足（已用 %dMB / 上限 %dMB），请删除旧作品或升级会员", usedMb, maxMb));
        }
    }

    /**
     * 检查当前用户已创建的项目数量是否达标
     *
     * @param user                用户实体
     * @param currentProjectCount 当前已有项目数
     * @throws com.soundread.common.exception.QuotaExceededException 如果超过上限抛出异常
     */
    public void checkProjectQuota(User user, int currentProjectCount) {
        TierPolicyDto.QuotaLimits limits = getQuotaLimits(user);
        int maxProjects = limits.getMaxProjects();
        if (maxProjects == -1) {
            return;
        }
        if (currentProjectCount >= maxProjects) {
            throw new com.soundread.common.exception.QuotaExceededException(
                    String.format("项目数量已达上限（%d/%d），请删除旧项目或升级会员", currentProjectCount, maxProjects));
        }
    }

    /**
     * 追加存储占用量
     *
     * @param userId    用户 ID
     * @param fileBytes 文件字节数
     */
    public void addStorage(Long userId, long fileBytes) {
        userStorageMapper.updateStorageDelta(userId, fileBytes, 1);
        log.debug("用户 {} 增加存储量 {} bytes", userId, fileBytes);
    }

    /**
     * 释放存储占用量
     *
     * @param userId    用户 ID
     * @param fileBytes 文件字节数
     */
    public void releaseStorage(Long userId, long fileBytes) {
        userStorageMapper.updateStorageDelta(userId, -fileBytes, -1);
        log.debug("用户 {} 释放存储量 {} bytes", userId, fileBytes);
    }

    /**
     * 获取用户已使用的存储量 (字节)
     *
     * @param userId 用户 ID
     * @return 已使用的字节数，若记录不存在返回 0
     */
    public long getUsedBytes(Long userId) {
        UserStorage storage = userStorageMapper.selectById(userId);
        return storage != null ? storage.getUsedBytes() : 0;
    }

    /**
     * 获取用户允许的最大存储容量 (字节)
     * <p>
     * 优先检查 user_storage.quota_override_mb 字段进行特殊覆盖配置。
     * </p>
     *
     * @param user 用户实体
     * @return 最大存储容量的字节数, 如果为无限制则返回 -1
     */
    private long getMaxStorageBytes(User user) {
        // 先检查是否存在单独授权覆盖
        UserStorage storage = userStorageMapper.selectById(user.getId());
        if (storage != null && storage.getQuotaOverrideMb() != null) {
            int overrideMb = storage.getQuotaOverrideMb();
            return overrideMb == -1 ? -1 : overrideMb * MB_TO_BYTES;
        }

        // 默认按等级读取全局策略
        TierPolicyDto.QuotaLimits limits = getQuotaLimits(user);
        int maxMb = limits.getStorageMaxMb();
        return maxMb == -1 ? -1 : maxMb * MB_TO_BYTES;
    }

    /**
     * 获取用户的会员配额限制策略配置
     */
    private TierPolicyDto.QuotaLimits getQuotaLimits(User user) {
        String tierCode = user.getTierCode() != null ? user.getTierCode() : "user";
        com.soundread.model.entity.SysTierPolicy policy = tierPolicyService.getByTierCode(tierCode);
        if (policy != null && policy.getQuotaLimits() != null) {
            return policy.getQuotaLimits();
        }
        return new TierPolicyDto.QuotaLimits();
    }
}
