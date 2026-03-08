package com.soundread.service;

import com.soundread.model.dto.AiPromptCategoryDTO;

import java.util.List;

/**
 * AI语音指令库服务接口
 */
public interface AiPromptLibraryService {

    /**
     * 获取带有嵌套角色列表的所有分类，用于前端UI展示
     * 
     * @return 嵌套DTO列表
     */
    List<AiPromptCategoryDTO> getAllCategoriesWithRoles();

}
