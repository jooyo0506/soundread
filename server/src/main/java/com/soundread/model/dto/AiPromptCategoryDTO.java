package com.soundread.model.dto;

import com.soundread.model.entity.AiPromptRole;
import lombok.Data;

import java.util.List;

/**
 * AI语音指令类别连带角色列表的DTO
 */
@Data
public class AiPromptCategoryDTO {

    private Long id;

    /**
     * 分类名称 (如: 广播剧)
     */
    private String category;

    /**
     * 分类图标 (如: fas fa-headphones-alt)
     */
    private String icon;

    private Integer sortOrder;

    /**
     * 该类别下的角色列表
     */
    private List<AiPromptRole> roles;
}
