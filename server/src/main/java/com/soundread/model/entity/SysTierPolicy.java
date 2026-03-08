package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.soundread.model.dto.TierPolicyDto;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会员等级策略字典表
 */
@Data
@TableName(value = "sys_tier_policy", autoResultMap = true)
public class SysTierPolicy {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 等级代号: guest, user, vip_month, vip_year, vip_lifetime */
    private String tierCode;

    /** 等级名称 */
    private String tierName;

    /** 功能开关 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private TierPolicyDto.FeatureFlags featureFlags;

    /** 配额限制 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private TierPolicyDto.QuotaLimits quotaLimits;

    /** 资源倾斜规则 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private TierPolicyDto.ResourceRules resourceRules;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
