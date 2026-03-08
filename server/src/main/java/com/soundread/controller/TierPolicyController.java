package com.soundread.controller;

import com.soundread.common.Result;
import com.soundread.model.entity.SysTierPolicy;
import com.soundread.service.TierPolicyService;
import com.soundread.mapper.TierPolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运营后台 — 等级策略管理控制器
 *
 * <p>
 * 提供对 sys_tier_policy 表的 CRUD 操作，
 * 支持运营端热更新等级策略（功能开关、配额上限、LLM 模型配置等）。
 * 修改后自动刷新缓存，秒级生效。
 * </p>
 *
 * @author SoundRead
 */
@RestController
@RequestMapping("/api/admin/policy")
@RequiredArgsConstructor
public class TierPolicyController {

    private final TierPolicyMapper tierPolicyMapper;
    private final TierPolicyService tierPolicyService;

    /**
     * 查询所有等级策略
     */
    @GetMapping("/list")
    public Result<List<SysTierPolicy>> list() {
        return Result.ok(tierPolicyMapper.selectList(null));
    }

    /**
     * 根据 ID 查询单条策略
     */
    @GetMapping("/{id}")
    public Result<SysTierPolicy> getById(@PathVariable Integer id) {
        return Result.ok(tierPolicyMapper.selectById(id));
    }

    /**
     * 更新策略 (核心运营操作)
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody SysTierPolicy policy) {
        policy.setId(id);
        tierPolicyMapper.updateById(policy);
        // 刷新缓存, 使配置秒级生效
        tierPolicyService.refreshAll();
        return Result.ok();
    }

    /**
     * 新增策略 (新增等级时使用)
     */
    @PostMapping
    public Result<Void> create(@RequestBody SysTierPolicy policy) {
        tierPolicyMapper.insert(policy);
        tierPolicyService.refreshAll();
        return Result.ok();
    }

    /**
     * 删除策略
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        tierPolicyMapper.deleteById(id);
        tierPolicyService.refreshAll();
        return Result.ok();
    }

    /**
     * 手动刷新缓存 (运维工具)
     */
    @PostMapping("/refresh")
    public Result<String> refresh() {
        tierPolicyService.refreshAll();
        return Result.ok("缓存已刷新");
    }
}
