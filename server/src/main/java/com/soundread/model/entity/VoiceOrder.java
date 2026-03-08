package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("voice_order")
public class VoiceOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String voiceId;

    private BigDecimal amount;
    private String payMethod;

    // pending, paid, cancelled, refunded
    private String status;
    private String tradeNo;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
