package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * TTS 异步任务表
 */
@Data
@TableName("tts_task")
public class TtsTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String inputText;

    private String voiceId;

    /** short / long / emotion / ai_script */
    private String taskType;

    /** pending / processing / completed / failed */
    private String status;

    private String audioUrl;

    private Integer audioDuration;

    /** 火山引擎返回的第三方任务ID */
    private String externalTaskId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
