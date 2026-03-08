package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创作内容分段实体 (章节/幕/段落)
 *
 * <p>
 * 每个 StudioProject 下有多个 Section，
 * 代表一个章节、一幕、一段对白等。
 * 每段可独立绑定角色和音色，独立合成音频。
 * </p>
 */
@Data
@TableName("studio_section")
public class StudioSection {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 studio_project.id */
    private Long projectId;

    /** 段序号 (0-based) */
    private Integer sectionIndex;

    /** 段标题 */
    private String title;

    /** 文本内容 */
    private String content;

    /** 角色名 (多角色时) */
    private String characterName;

    /** 绑定的音色ID */
    private String voiceId;

    /** 情感标签 */
    private String emotionTag;

    /** 该段音频URL */
    private String audioUrl;

    /** AI 摘要（摘要压缩记忆链：≤50字概括本段核心内容，供续写时注入上下文） */
    private String summary;

    /** 状态: pending/generated/synthesized */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
