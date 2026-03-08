package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作品表 (发现页内容 + 运营审核)
 */
@Data
@TableName("work")
public class Work {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 关联 user_creation.id */
    private Long creationId;

    private String title;

    /** hot / latest / emotion / story / news / podcast / novel */
    private String category;

    /** tts / emotion / drama / podcast / novel */
    private String sourceType;

    /** 内容类型: audio / novel / podcast / music */
    private String contentType;

    /** 内容描述/摘要 */
    private String description;

    /** 字数（小说用） */
    private Integer wordCount;

    /** 章节数（小说用） */
    private Integer chapterCount;

    /** 关联的 studio_project.id */
    private Long sourceProjectId;

    /** 扩展字段 JSON */
    private String extraJson;

    private String coverUrl;

    private String audioUrl;

    private Integer audioDuration;

    private Integer playCount;

    private Integer likeCount;

    private Integer shareCount;

    private Integer commentCount;

    /** AI 生成的内容摘要 (用于语义推荐) */
    private String aiSummary;

    /** 向量 ID (Milvus/Qdrant 中对应的向量) */
    private String vectorId;

    /** draft / published / pending / approved / rejected */
    private String status;

    /** pending / approved / rejected */
    private String reviewStatus;

    private String reviewNote;

    private LocalDateTime reviewedAt;

    private Long reviewedBy;

    /** 运营精选 */
    private Integer isFeatured;

    /** 热度分 */
    private BigDecimal heatScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
