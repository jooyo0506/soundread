package com.soundread.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soundread.common.Result;
import com.soundread.model.entity.Work;
import com.soundread.service.AdminWorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 运营后台 — 作品审核管理控制器
 *
 * <p>
 * 提供作品列表查询（多维筛选）、审核操作（approve/reject）、
 * 精选推荐切换、看板统计等管理能力。
 * 所有接口均受 Sa-Token admin 角色校验保护。
 * </p>
 *
 * @author SoundRead
 */
@RestController
@RequestMapping("/api/admin/works")
@RequiredArgsConstructor
public class AdminWorkController {

    private final AdminWorkService adminWorkService;

    /**
     * 作品列表（分页 + 筛选）
     *
     * @param reviewStatus 审核状态 (pending/approved/rejected)
     * @param sourceType   来源类型 (tts/emotion/novel 等)
     * @param keyword      标题关键词
     * @param page         页码 (从1开始)
     * @param size         每页条数
     */
    @GetMapping("/list")
    public Result<Page<Work>> list(
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminWorkService.listWorks(reviewStatus, sourceType, keyword, page, size));
    }

    /**
     * 审核作品
     *
     * @param id   作品ID
     * @param body 包含 action (approve/reject) 和 reviewNote
     */
    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String action = body.get("action");
        String note = body.getOrDefault("reviewNote", "");
        Long reviewerId = StpUtil.getLoginIdAsLong();
        adminWorkService.reviewWork(id, action, note, reviewerId);
        return Result.ok();
    }

    /**
     * 切换精选状态
     */
    @PutMapping("/{id}/feature")
    public Result<Void> toggleFeatured(@PathVariable Long id) {
        adminWorkService.toggleFeatured(id);
        return Result.ok();
    }

    /**
     * 删除作品
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminWorkService.deleteWork(id);
        return Result.ok();
    }

    /**
     * 上架/下架作品
     *
     * @param id   作品ID
     * @param body 包含 status (published/unpublished)
     */
    @PutMapping("/{id}/publish")
    public Result<Void> togglePublish(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminWorkService.togglePublish(id, body.get("status"));
        return Result.ok();
    }

    /**
     * 运营看板统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminWorkService.getStats());
    }
}
