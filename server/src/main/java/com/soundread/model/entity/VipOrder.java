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

    private Long userId;

    /** month / year / lifetime */
    private String planId;

    private BigDecimal amount;

    /** wechat / alipay */
    private String payMethod;

    /** pending / paid / cancelled / refunded */
    private String status;

    private String tradeNo;

    private LocalDateTime paidAt;

    private LocalDateTime expireAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
