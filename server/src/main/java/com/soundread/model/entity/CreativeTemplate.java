package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创作类型模板 — 运营可配的内容形态定义
 *
 * <p>
 * 每种创作类型（AI小说创作/广播剧/播客/电台/讲解/带货/绘本/新闻）
 * 对应一条记录，包含 AI 角色 Prompt、推荐音色、设置项等配置。
 * 新增创作形态只需 INSERT 一行，不用改代码。
 * </p>
 */
@Data
@TableName("creative_template")
public class CreativeTemplate {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 类型编码: novel/drama/podcast/radio/lecture/ad/picture_book/news */
    private String typeCode;

    /** 类型名称 */
    private String typeName;

    /** 图标 emoji */
    private String icon;

    /** 类型描述 */
    private String description;

    /** AI 角色 System Prompt */
    private String aiRole;

    /** 输出格式: text/dialogue/script */
    private String outputFormat;

    /** 推荐音色配置 JSON */
    private String defaultVoices;

    /** 该类型支持的设置项 JSON */
    private String settingsSchema;

    /** 排序权重 */
    private Integer sortOrder;

    /** 是否启用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
