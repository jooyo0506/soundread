package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 克隆音色表
 */
@Data
@TableName("cloned_voice")
public class ClonedVoice {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** R2 上的样本音频地址 */
    private String sampleUrl;

    /** 克隆后的音色 ID */
    private String clonedVoiceId;

    private String voiceName;

    /** 样本时长 (秒) */
    private Integer sampleDuration;

    /** training / ready / failed */
    private String status;

    /** AI 检测的音频质量分 (0-100) */
    private Integer qualityScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
