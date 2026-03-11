package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String phone;

    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    /**
     * VIP等级: 0=免费, 1=月度, 2=年度, 3=终身
     */
    private Integer vipLevel;

    private LocalDateTime vipExpireTime;

    /**
     * 当前使用的策略等级代号 (guest/user/vip_month等)
     */
    @TableField("tier_code")
    private String tierCode;

    /**
     * 系统角色 (user/admin)
     */
    @TableField("role")
    private String role;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    /**
     * 判断用户是否为有效 VIP
     *
     * <p>
     * 判断逻辑:
     * </p>
     * <ul>
     * <li>vipLevel 为 null 或 0 → 非 VIP</li>
     * <li>vipLevel 为 3 → 终身 VIP（无需检查过期时间）</li>
     * <li>其他等级 → 检查 vipExpireTime 是否在当前时间之后</li>
     * </ul>
     *
     * @return true=有效 VIP, false=非 VIP 或已过期
     */
    public boolean isVip() {
        // 优先判断 tierCode（运营端只改 tier_code 时也能正确识别）
        if (tierCode != null && tierCode.startsWith("vip_")) {
            if ("vip_lifetime".equals(tierCode))
                return true;
            // 有过期时间则校验，没有过期时间则直接认可
            if (vipExpireTime != null)
                return vipExpireTime.isAfter(LocalDateTime.now());
            return true;
        }
        // 兼容旧 vipLevel 逻辑
        if (vipLevel == null || vipLevel == 0)
            return false;
        if (vipLevel == 3)
            return true;
        return vipExpireTime != null && vipExpireTime.isAfter(LocalDateTime.now());
    }
}
