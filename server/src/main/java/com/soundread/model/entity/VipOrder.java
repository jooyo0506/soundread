package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP 订单表
 */
@Data
@TableName("vip_order")
public class VipOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统订单号（全局唯一，雪花ID字符串） */
    private String orderNo;

    private Long userId;

    /** vip_month / vip_year / vip_lifetime */
    private String planId;

    /** 套餐名称快照 */
    private String planName;

    private BigDecimal amount;

    /** 套餐时长（天，9999=永久） */
    private Integer durationDays;

    /** pending / paid / failed / refunded */
    private String status;

    /** 支付宝交易号 */
    private String alipayTradeNo;

    /** 支付完成时间 */
    private LocalDateTime payTime;

    /** 支付宝原始回调报文（调试用） */
    private String notifyRaw;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
