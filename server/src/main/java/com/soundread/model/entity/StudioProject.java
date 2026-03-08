package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创作项目实体
 *
 * <p>
 * 每个用户的每次创作会话对应一个 StudioProject。
 * 包含灵感输入、AI 生成的大纲、角色设定、状态等元数据。
 * </p>
 */
@Data
@TableName("studio_project")
public class StudioProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联 creative_template.id */
    private Integer templateId;

    /** 冗余: 类型编码 */
    private String typeCode;

    /** 项目标题 */
    private String title;

    /** 用户输入的灵感/大纲 */
    private String inspiration;

    /** AI 生成的大纲 (JSON) */
    private String outline;

    /** 角色设定 JSON数组: [{name,desc,voiceId}] */
    private String characters;

    /** 状态: draft/creating/editing/completed */
    private String status;

    /** 总段/章/幕数 */
    private Integer totalSections;

    /** 最终合成音频URL */
    private String audioUrl;

    /** 音频时长(秒) */
    private Integer audioDuration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
