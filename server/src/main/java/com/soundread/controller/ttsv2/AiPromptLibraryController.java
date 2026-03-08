package com.soundread.controller.ttsv2;

import com.soundread.common.Result;
import com.soundread.model.dto.AiPromptCategoryDTO;
import com.soundread.service.AiPromptLibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI语音指令库模块端点
 */
@Slf4j
@RestController
@RequestMapping("/api/tts/v2/prompt-library")
@RequiredArgsConstructor
public class AiPromptLibraryController {

    private final AiPromptLibraryService aiPromptLibraryService;

    /**
     * 获取全部分类与角色（树形结构）
     */
    @GetMapping("/tree")
    public Result<List<AiPromptCategoryDTO>> getPromptLibraryTree() {
        try {
            List<AiPromptCategoryDTO> tree = aiPromptLibraryService.getAllCategoriesWithRoles();
            return Result.ok(tree);
        } catch (Exception e) {
            log.error("获取指令库失败", e);
            return Result.fail("获取指令库失败: " + e.getMessage());
        }
    }
}
