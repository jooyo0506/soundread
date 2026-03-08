package com.soundread.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soundread.common.Result;
import com.soundread.model.entity.Work;
import com.soundread.service.ContentService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 发现/社区 Controller
 */
@RestController
@RequestMapping("/api/discover")
@RequiredArgsConstructor
public class DiscoverController {

    private final ContentService contentService;

    @GetMapping("/banners")
    public Result<List<Work>> getBanners() {
        return Result.ok(contentService.getBanners());
    }

    @GetMapping("/works")
    public Result<Page<Work>> getWorks(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        // contentType 优先（前端使用此字段），category 作为兼容
        String filterCategory = (contentType != null && !contentType.isEmpty()) ? contentType : category;
        return Result.ok(contentService.getWorks(filterCategory, sort, page, size));
    }

    @PostMapping("/works/{id}/play")
    public Result<Void> play(@PathVariable Long id) {
        contentService.incrementPlayCount(id);
        return Result.ok();
    }

    @PostMapping("/works/{id}/like")
    public Result<Boolean> like(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        boolean liked = contentService.toggleLike(userId, id);
        return Result.ok(liked);
    }

    /**
     * 下架自己的作品（作者操作）
     */
    @PostMapping("/works/{id}/unpublish")
    public Result<?> unpublishWork(@PathVariable Long id) {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            contentService.unpublishWork(id, userId);
            return Result.ok("作品已下架");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 重新上架自己的作品（作者操作）
     */
    @PostMapping("/works/{id}/republish")
    public Result<?> republishWork(@PathVariable Long id) {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            contentService.republishWork(id, userId);
            return Result.ok("作品已重新上架");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}
