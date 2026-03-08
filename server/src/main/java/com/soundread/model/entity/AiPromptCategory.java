package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI语音指令类别
 */
@Data
@TableName("ai_prompt_category")
public class AiPromptCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称 (如: 广播剧)
     */
    private String name;

    /**
     * 分类图标 (如: fas fa-headphones-alt)
     */
    private String icon;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
