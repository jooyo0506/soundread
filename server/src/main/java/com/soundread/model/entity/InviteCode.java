package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请码实体
 */
@Data
@TableName("invite_code")
public class InviteCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 邀请码（大写字母+数字） */
    private String code;

    /** 最大可用次数，-1 表示无限 */
    private Integer maxUses;

    /** 已使用次数 */
    private Integer usedCount;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 过期时间，null = 永不过期 */
    private LocalDateTime expiredAt;

    /**
     * 判断该邀请码是否仍可使用
     */
    public boolean isAvailable() {
        // 检查是否过期
        if (expiredAt != null && expiredAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        // -1 表示无限次
        if (maxUses == -1) {
            return true;
        }
        return usedCount < maxUses;
    }
}
