package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 交互记录 (边听边问)
 */
@Data
@TableName("ai_interaction")
public class AiInteraction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 会话 ID，同一次收听中的多轮对话 */
    private String sessionId;

    /** 关联的作品 ID */
    private Long contextWorkId;

    /** 用户问题 (ASR 转写) */
    private String question;

    /** AI 回答 */
    private String answer;

    /** 回答音频 URL */
    private String answerAudioUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
