package com.soundread.controller;

import com.soundread.common.Result;
import com.soundread.model.entity.MusicTask;
import com.soundread.service.MusicService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 音乐 Controller
 *
 * <p>
 * 提供音乐生成、歌词生成、任务查询、任务列表、删除任务等接口。
 * </p>
 *
 * @author SoundRead
 */
@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    /**
     * 提交音乐生成任务
     */
    @PostMapping("/generate")
    public Result<?> generate(@RequestBody GenerateRequest req) {
        try {
            MusicTask task = musicService.submitGenerate(
                    req.getType(), req.getPrompt(), req.getLyrics(), req.getModel());
            return Result.ok(Map.of(
                    "taskId", String.valueOf(task.getId()),
                    "status", task.getStatus()));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * AI 生成歌词
     */
    @PostMapping("/lyrics")
    public Result<?> generateLyrics(@RequestBody LyricsRequest req) {
        try {
            var result = musicService.generateLyrics(req.getPrompt());
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/task/{id}")
    public Result<?> getTask(@PathVariable Long id) {
        try {
            return Result.ok(musicService.getTask(id));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 用户任务列表
     */
    @GetMapping("/list")
    public Result<?> listTasks() {
        return Result.ok(musicService.listMyTasks());
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/task/{id}")
    public Result<?> deleteTask(@PathVariable Long id) {
        try {
            musicService.deleteTask(id);
            return Result.ok("删除成功");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 发布音乐到灵感大厅
     */
    @PostMapping("/task/{id}/publish")
    public Result<?> publishTask(@PathVariable Long id) {
        try {
            var result = musicService.publishTask(id);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 重命名音乐
     */
    @PostMapping("/task/{id}/rename")
    public Result<?> renameTask(@PathVariable Long id, @RequestBody RenameRequest req) {
        try {
            musicService.renameTask(id, req.getTitle());
            return Result.ok("重命名成功");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 下架音乐（从灵感大厅移除）
     */
    @PostMapping("/task/{id}/unpublish")
    public Result<?> unpublishTask(@PathVariable Long id) {
        try {
            musicService.unpublishTask(id);
            return Result.ok("已下架");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    // ==================== DTO ====================

    @Data
    public static class GenerateRequest {
        /** song / instrumental */
        private String type;
        /** 风格提示词 */
        private String prompt;
        /** 歌词 (song 类型必填) */
        private String lyrics;
        /** 模型 (auto / mureka-7.5 / mureka-o1) */
        private String model;
    }

    @Data
    public static class LyricsRequest {
        /** 歌词描述 */
        private String prompt;
    }

    @Data
    public static class RenameRequest {
        private String title;
    }
}
