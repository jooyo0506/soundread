package com.soundread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.soundread.mapper.AiPromptCategoryMapper;
import com.soundread.mapper.AiPromptRoleMapper;
import com.soundread.model.dto.AiPromptCategoryDTO;
import com.soundread.model.entity.AiPromptCategory;
import com.soundread.model.entity.AiPromptRole;
import com.soundread.service.AiPromptLibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptLibraryServiceImpl implements AiPromptLibraryService {

    private final AiPromptCategoryMapper categoryMapper;
    private final AiPromptRoleMapper roleMapper;

    @Override
    public List<AiPromptCategoryDTO> getAllCategoriesWithRoles() {
        // 1. 获取所有分类 (按sortOrder升序)
        QueryWrapper<AiPromptCategory> catQuery = new QueryWrapper<>();
        catQuery.orderByAsc("sort_order");
        List<AiPromptCategory> categories = categoryMapper.selectList(catQuery);

        // 2. 获取所有角色 (按sortOrder升序)
        QueryWrapper<AiPromptRole> roleQuery = new QueryWrapper<>();
        roleQuery.orderByAsc("sort_order");
        List<AiPromptRole> allRoles = roleMapper.selectList(roleQuery);

        // 3. 将角色按 categoryId 分组
        Map<Long, List<AiPromptRole>> rolesByCategoryId = allRoles.stream()
                .collect(Collectors.groupingBy(AiPromptRole::getCategoryId));

        // 4. 组装为 DTO 返回
        return categories.stream().map(cat -> {
            AiPromptCategoryDTO dto = new AiPromptCategoryDTO();
            dto.setId(cat.getId());
            dto.setCategory(cat.getName());
            dto.setIcon(cat.getIcon());
            dto.setSortOrder(cat.getSortOrder());
            dto.setRoles(rolesByCategoryId.getOrDefault(cat.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());
    }
}
