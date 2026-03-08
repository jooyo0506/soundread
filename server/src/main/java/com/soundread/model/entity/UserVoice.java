package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_voice")
public class UserVoice {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String voiceId;

    // 获取途径: purchased, gift, activity
    private String obtainWay;

    // 过期时间, NULL表示永久专属
    private LocalDateTime expireTime;

    private LocalDateTime createdAt;
}
