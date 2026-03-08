package com.soundread.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI语音指令明细
 */
@Data
@TableName("ai_prompt_role")
public class AiPromptRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 归属分类的ID
     */
    private Long categoryId;

    /**
     * 角色名称 (如: 悬疑惊悚)
     */
    private String name;

    /**
     * 前端UI展示的短描述 (如: 压低声音、带点气声、放缓、偶尔颤音)
     */
    private String description;

    /**
     * 实际传给大模型的详细Prompt Tags
     */
    private String tags;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
