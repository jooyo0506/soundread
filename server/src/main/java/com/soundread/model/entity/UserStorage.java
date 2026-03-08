package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户存储配额记录实体（对应 user_storage 表）
 *
 * <p>
 * 记录每个用户当前在 R2 存储中已使用的字节数与文件数，
 * 由 {@code StorageQuotaService} 在文件上传/删除时实时增减维护。
 * 支持通过 {@code quota_override_mb} 字段为单个用户设置个性化配额覆盖，
 * 优先级高于 sys_tier_policy 中的等级限制。
 * </p>
 *
 * @author SoundRead
 */
@Data
@TableName("user_storage")
public class UserStorage {

   /** 用户 ID（与 user.id 一一对应，作为主键） */
   @TableId(type = IdType.INPUT)
   private Long userId;

   /** 已使用的存储字节数（随文件上传/删除实时更新） */
   private Long usedBytes;

   /** 已上传的文件总数（与 usedBytes 同步维护） */
   private Integer fileCount;

   /**
    * 个人配额覆盖值（单位：MB）
    * <p>
    * 为 null 时读取 sys_tier_policy.quota_limits.storage_max_mb 作为默认配额；
    * 设置为 -1 表示该用户享有无限存储空间（通常用于运营特殊授权或开发测试）。
    * </p>
    */
   private Integer quotaOverrideMb;

   /** 存储统计最后计算时间（用于定期重新汇总修正数据） */
   private LocalDateTime lastCalculatedAt;
}
