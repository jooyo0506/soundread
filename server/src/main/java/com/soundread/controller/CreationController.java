package com.soundread.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.soundread.common.Result;
import com.soundread.model.entity.UserCreation;
import com.soundread.model.entity.UserStorage;
import com.soundread.mapper.UserStorageMapper;
import com.soundread.model.entity.User;
import com.soundread.service.AuthService;
import com.soundread.service.CreationService;
import com.soundread.service.StorageQuotaService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 创作记录管理 Controller
 *
 * <p>
 * 提供用户创作记录（UserCreation）的增删改查接口，包含：
 * <ul>
 * <li>分页查询创作列表，支持按类型筛选</li>
 * <li>删除、重命名创作记录</li>
 * <li>发布 / 下架作品（同步写入 Work 表）</li>
 * <li>查询当前用户存储配额使用情况</li>
 * </ul>
 * </p>
 *
 * @author SoundRead
 */
@RestController
@RequestMapping("/api/creation")
@RequiredArgsConstructor
public class CreationController {

    private final CreationService creationService;
    private final StorageQuotaService storageQuotaService;
    private final UserStorageMapper userStorageMapper;
    private final AuthService authService;
    private final com.soundread.service.TierPolicyService tierPolicyService;

    /**
     * 分页查询当前用户的创作记录
     *
     * @param type 类型过滤（null=全部，可选值：tts / emotion / drama / podcast / novel 等）
     * @param page 页码（默认第 1 页）
     * @param size 每页数量（默认 20 条）
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<IPage<UserCreation>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(creationService.listByUser(userId, type, page, size));
    }

    /**
     * 删除指定创作记录（逻辑删除，同时释放存储配额）
     *
     * @param id 创作记录 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        creationService.delete(id, userId);
        return Result.ok(null);
    }

    /**
     * 将创作记录发布为公开作品
     *
     * <p>
     * 在 Work 表中创建对应记录，并将 isPublished 标记为已发布。
     * </p>
     *
     * @param id  创作记录 ID
     * @param req 发布请求（包含标题和作品分类）
     * @return 新建 Work 的 ID
     */
    @PostMapping("/{id}/publish")
    public Result<Long> publish(@PathVariable Long id, @RequestBody PublishRequest req) {
        User user = authService.getCurrentUser();
        Long workId = creationService.publish(id, req.getTitle(), req.getCategory(), user);
        return Result.ok(workId);
    }

    /**
     * 重命名创作记录标题
     *
     * @param id  创作记录 ID
     * @param req 重命名请求（包含新标题）
     */
    @PutMapping("/{id}/rename")
    public Result<Void> rename(@PathVariable Long id, @RequestBody RenameRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        creationService.rename(id, req.getTitle(), userId);
        return Result.ok(null);
    }

    /**
     * 下架已发布的作品（将对应 Work 状态改为 unpublished）
     *
     * @param id 创作记录 ID
     */
    @PostMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        creationService.unpublish(id, userId);
        return Result.ok(null);
    }

    /**
     * 查询当前用户的存储配额使用详情
     *
     * <p>
     * 若历史 usedBytes 记录为 0 但创作数量 &gt; 0，说明存储统计数据缺失，
     * 将自动重新汇总计算并回写到 user_storage 表中，避免前端显示错误。
     * </p>
     *
     * @return 包含已用字节数、文件数、最大配额、创作总数的存储信息
     */
    @GetMapping("/storage")
    public Result<StorageInfo> getStorageInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserStorage storage = userStorageMapper.selectById(userId);

        long usedBytes = storage != null ? storage.getUsedBytes() : 0;
        int fileCount = storage != null ? storage.getFileCount() : 0;
        long creationCount = creationService.countByUser(userId);

        // usedBytes 为 0 但创作数不为空，说明存储统计丢失，需重新汇总
        if (usedBytes == 0 && creationCount > 0) {
            long recalcBytes = creationService.sumFileSizeByUser(userId);
            if (recalcBytes > 0) {
                userStorageMapper.updateStorageDelta(userId, recalcBytes, (int) creationCount);
                usedBytes = recalcBytes;
                fileCount = (int) creationCount;
            }
        }

        StorageInfo info = new StorageInfo();
        info.setUsedBytes(usedBytes);
        info.setFileCount(fileCount);

        // 根据用户等级策略或个人配额覆盖获取最大可用空间
        User user = authService.getCurrentUser();
        long maxBytes = getMaxBytesForDisplay(user, storage);
        info.setMaxBytes(maxBytes);
        info.setCreationCount(creationCount);

        return Result.ok(info);
    }

    // ==================== 内部 DTO ====================

    @Data
    public static class PublishRequest {
        /** 作品标题 */
        private String title;
        /** 作品分类（如 novel / podcast / emotion 等） */
        private String category;
    }

    @Data
    public static class RenameRequest {
        /** 新标题 */
        private String title;
    }

    @Data
    public static class StorageInfo {
        /** 已使用的存储字节数 */
        private long usedBytes;
        /** 最大可用配额字节数（-1 表示无限制） */
        private long maxBytes;
        /** 已上传文件总数 */
        private int fileCount;
        /** 创作记录总数 */
        private long creationCount;
    }

    /**
     * 根据用户配额策略计算前端展示用的最大存储字节数
     *
     * <p>
     * 优先使用 user_storage.quota_override_mb 个人配额覆盖值；
     * 若未设置则读取用户等级对应的 sys_tier_policy 策略，默认 50MB。
     * -1 表示无限制存储。
     * </p>
     *
     * @param user    当前用户实体
     * @param storage 用户存储记录（可为 null）
     * @return 最大配额字节数，-1 表示不限
     */
    private long getMaxBytesForDisplay(User user, UserStorage storage) {
        if (storage != null && storage.getQuotaOverrideMb() != null) {
            int mb = storage.getQuotaOverrideMb();
            return mb == -1 ? -1 : mb * 1024L * 1024L;
        }
        String tierCode = user.getTierCode() != null ? user.getTierCode() : "user";
        var policy = tierPolicyService.getByTierCode(tierCode);
        int maxMb = (policy != null && policy.getQuotaLimits() != null)
                ? policy.getQuotaLimits().getStorageMaxMb()
                : 50;
        return maxMb == -1 ? -1 : maxMb * 1024L * 1024L;
    }
}
