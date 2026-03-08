package com.soundread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soundread.mapper.TierPolicyMapper;
import com.soundread.model.dto.TierPolicyDto;
import com.soundread.model.entity.SysTierPolicy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 等级策略服务
 * <p>
 * 核心职责:
 * 1. 启动时 / 定时 将 sys_tier_policy 全量加载到本地内存 + Redis
 * 2. 根据 tierCode 查询对应策略 (O(1) 内存命中)
 * 3. 提供热刷新能力 (运营修改数据库后调用 refresh 即可)
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TierPolicyService {

    private final TierPolicyMapper tierPolicyMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    /** 本地一级缓存: tierCode -> 策略实体 */
    private final Map<String, SysTierPolicy> localCache = new ConcurrentHashMap<>();

    private static final String REDIS_KEY_PREFIX = "sys:policy:";

    /**
     * 应用启动时自动预热缓存
     */
    @PostConstruct
    public void init() {
        refreshAll();
    }

    /**
     * 全量刷新缓存 (启动时调用 / 运营后台修改后手动调用)
     */
    public void refreshAll() {
        List<SysTierPolicy> policies = tierPolicyMapper.selectList(null);
        localCache.clear();
        for (SysTierPolicy policy : policies) {
            localCache.put(policy.getTierCode(), policy);
            // 同步写入 Redis (多节点环境下其他服务也可读)
            try {
                redisTemplate.opsForValue().set(
                        REDIS_KEY_PREFIX + policy.getTierCode(),
                        objectMapper.writeValueAsString(policy));
            } catch (Exception e) {
                log.warn("写入 Redis 策略缓存失败: tierCode={}", policy.getTierCode(), e);
            }
        }
        log.info("等级策略缓存已刷新, 共加载 {} 条", policies.size());

        // 联动清空 LLM 模型路由缓存, 下次请求将基于新策略重建模型实例
        try {
            applicationContext.getBean(com.soundread.config.ai.LlmRouter.class).invalidateCache();
        } catch (Exception e) {
            log.debug("LlmRouter 尚未初始化, 跳过缓存清理");
        }
    }

    /**
     * 根据 tierCode 获取策略 (先查本地缓存, 命中率极高)
     */
    public SysTierPolicy getByTierCode(String tierCode) {
        if (tierCode == null || tierCode.isBlank()) {
            tierCode = "user"; // 缺省等级
        }
        SysTierPolicy cached = localCache.get(tierCode);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中则回查数据库
        SysTierPolicy policy = tierPolicyMapper.selectOne(
                new LambdaQueryWrapper<SysTierPolicy>().eq(SysTierPolicy::getTierCode, tierCode));
        if (policy != null) {
            localCache.put(tierCode, policy);
        }
        return policy;
    }

    /**
     * 组装前端需要的聚合策略体 (登录后一次性下发)
     */
    public TierPolicyDto.UserPolicy buildUserPolicy(String tierCode) {
        SysTierPolicy policy = getByTierCode(tierCode);
        TierPolicyDto.UserPolicy up = new TierPolicyDto.UserPolicy();
        if (policy != null) {
            up.setTierCode(policy.getTierCode());
            up.setTierName(policy.getTierName());
            up.setFeatureFlags(policy.getFeatureFlags());
            up.setQuotaLimits(policy.getQuotaLimits());
            up.setResourceRules(policy.getResourceRules());
        } else {
            // 兜底: 返回最保守的默认策略
            up.setTierCode("user");
            up.setTierName("普通用户");
            up.setFeatureFlags(new TierPolicyDto.FeatureFlags());
            up.setQuotaLimits(new TierPolicyDto.QuotaLimits());
            up.setResourceRules(new TierPolicyDto.ResourceRules());
        }
        return up;
    }

    /**
     * 检查指定 tierCode 是否拥有某个功能的权限
     */
    public boolean hasFeature(String tierCode, String featureName) {
        SysTierPolicy policy = getByTierCode(tierCode);
        if (policy == null || policy.getFeatureFlags() == null)
            return false;

        TierPolicyDto.FeatureFlags flags = policy.getFeatureFlags();
        return switch (featureName) {
            case "tts_basic" -> flags.isTtsBasic();
            case "tts_emotion_v2" -> flags.isTtsEmotionV2();
            case "ai_podcast" -> flags.isAiPodcast();
            case "voice_clone" -> flags.isVoiceClone();
            case "ai_script" -> flags.isAiScript();
            case "multi_language" -> flags.isMultiLanguage();
            case "ai_novel" -> flags.isAiNovel();
            case "auto_publish" -> flags.isAutoPublish();
            default -> false;
        };
    }

    /**
     * 获取指定等级的资源分配规则 (供 LlmRouter 等组件调用)
     */
    public TierPolicyDto.ResourceRules getResourceRules(String tierCode) {
        SysTierPolicy policy = getByTierCode(tierCode);
        if (policy == null || policy.getResourceRules() == null) {
            return new TierPolicyDto.ResourceRules(); // 使用默认值
        }
        return policy.getResourceRules();
    }
}
