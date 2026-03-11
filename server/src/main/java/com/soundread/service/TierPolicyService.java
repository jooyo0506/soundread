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
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 等级策略服务
 *
 * <h3>两级缓存架构</h3>
 * 
 * <pre>
 * L1: JVM 本地 ConcurrentHashMap（O(1)，无网络开销）
 * L2: Redis（sys:policy:{tierCode}，多节点共享，无 TTL）
 * DB: sys_tier_policy 表（兜底）
 * </pre>
 *
 * <h3>跨节点缓存一致性</h3>
 * <p>
 * 问题：运营端调用 refreshAll() 时，只能清自己 JVM 的 localCache；
 * 生产服务器的 localCache 不受影响，导致读到旧策略。
 * </p>
 * <p>
 * 解决：refreshAll() 往 Redis 发布 {@code soundread:policy:refresh} 消息；
 * 所有订阅此 Topic 的节点（含生产服务器）收到消息后自动清除本地缓存，
 * 下次请求从 Redis/DB 重新加载。
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

    /** L1 本地缓存: tierCode → 策略实体 */
    private final Map<String, SysTierPolicy> localCache = new ConcurrentHashMap<>();

    private static final String REDIS_KEY_PREFIX = "sys:policy:";

    /**
     * Redis Pub/Sub 频道：运营端刷新策略后广播，所有节点自动清除 L1 缓存
     * 运营端 refreshAll() 发布消息 → 生产服务器监听 → 清 localCache
     */
    public static final String REFRESH_CHANNEL = "soundread:policy:refresh";

    /**
     * 应用启动时自动预热缓存（从 DB 加载）
     */
    @PostConstruct
    public void init() {
        refreshAll();
        // 订阅策略刷新消息（多节点一致性）
        subscribeRefreshChannel();
    }

    /**
     * 全量刷新缓存（启动时 / 运营后台修改后手动调用）
     *
     * <p>
     * 执行步骤：
     * 1. 从 DB 读取最新策略
     * 2. 清空并重建本地 L1 缓存
     * 3. 同步写入 Redis（其他节点可读）
     * 4. 发布 Pub/Sub 消息，通知其他节点清除 L1 缓存
     * </p>
     */
    public void refreshAll() {
        List<SysTierPolicy> policies = tierPolicyMapper.selectList(null);
        localCache.clear();
        for (SysTierPolicy policy : policies) {
            localCache.put(policy.getTierCode(), policy);
            // 同步写入 Redis（多节点环境下其他服务重建 L1 时读取）
            try {
                redisTemplate.opsForValue().set(
                        REDIS_KEY_PREFIX + policy.getTierCode(),
                        objectMapper.writeValueAsString(policy));
            } catch (Exception e) {
                log.warn("写入 Redis 策略缓存失败: tierCode={}", policy.getTierCode(), e);
            }
        }
        log.info("等级策略缓存已刷新, 共加载 {} 条", policies.size());

        // 发布 Pub/Sub 消息，通知所有节点（含生产服务器）清除本地 L1 缓存
        try {
            redisTemplate.convertAndSend(REFRESH_CHANNEL, "refresh:" + System.currentTimeMillis());
            log.info("[策略缓存] 已发布刷新通知 → 频道: {}", REFRESH_CHANNEL);
        } catch (Exception e) {
            log.warn("[策略缓存] 发布刷新通知失败（不影响功能）: {}", e.getMessage());
        }

        // 联动清空 LLM 模型路由缓存，下次请求基于新策略重建模型实例
        try {
            applicationContext.getBean(com.soundread.config.ai.LlmRouter.class).invalidateCache();
        } catch (Exception e) {
            log.debug("LlmRouter 尚未初始化, 跳过缓存清理");
        }
    }

    /**
     * 根据 tierCode 获取策略
     *
     * <p>
     * 查询顺序：L1 本地缓存 → Redis → DB（逐层降级）
     * </p>
     */
    public SysTierPolicy getByTierCode(String tierCode) {
        if (tierCode == null || tierCode.isBlank()) {
            tierCode = "user";
        }

        // 1. 查 L1 本地缓存
        SysTierPolicy cached = localCache.get(tierCode);
        if (cached != null) {
            return cached;
        }

        // 2. L1 Cache Miss → 查 Redis（运营端刷新后数据在 Redis，L1 被清空后走到这里）
        try {
            String json = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + tierCode);
            if (json != null) {
                SysTierPolicy policy = objectMapper.readValue(json, SysTierPolicy.class);
                localCache.put(tierCode, policy); // 回填 L1
                log.debug("[TierPolicy] Redis 命中 → 回填 L1: tierCode={}", tierCode);
                return policy;
            }
        } catch (Exception e) {
            log.warn("[TierPolicy] 读取 Redis 失败，降级查 DB: tierCode={}, err={}", tierCode, e.getMessage());
        }

        // 3. Redis Miss → 查 DB
        SysTierPolicy policy = tierPolicyMapper.selectOne(
                new LambdaQueryWrapper<SysTierPolicy>().eq(SysTierPolicy::getTierCode, tierCode));
        if (policy != null) {
            localCache.put(tierCode, policy);
        }
        return policy;
    }

    /**
     * 订阅 Redis Pub/Sub 频道，接收其他节点（运营端）的缓存刷新通知
     *
     * <p>
     * 当运营端 refreshAll() 发布消息后，生产服务器收到通知，
     * 清除本地 L1 缓存，下次 getByTierCode() 会从 Redis 读取最新数据。
     * </p>
     */
    private void subscribeRefreshChannel() {
        try {
            RedisMessageListenerContainer container = applicationContext.getBean(RedisMessageListenerContainer.class);
            container.addMessageListener(
                    (message, pattern) -> {
                        log.info("[策略缓存] 收到刷新通知，清除本地 L1 缓存");
                        localCache.clear();
                        // 联动清 LLM Router 缓存
                        try {
                            applicationContext.getBean(com.soundread.config.ai.LlmRouter.class)
                                    .invalidateCache();
                        } catch (Exception ignored) {
                        }
                    },
                    new ChannelTopic(REFRESH_CHANNEL));
            log.info("[策略缓存] 已订阅刷新频道: {}", REFRESH_CHANNEL);
        } catch (Exception e) {
            log.warn("[策略缓存] 订阅失败（单节点模式，可忽略）: {}", e.getMessage());
        }
    }

    /**
     * 组装前端需要的聚合策略体（登录后一次性下发）
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
            case "ai_drama" -> flags.isAiDrama();
            case "ai_script" -> flags.isAiScript();
            case "ai_novel" -> flags.isAiNovel();
            case "ai_music" -> flags.isAiMusic();
            case "auto_publish" -> flags.isAutoPublish();
            default -> false;
        };
    }

    /**
     * 获取指定等级的资源分配规则（供 LlmRouter 等组件调用）
     */
    public TierPolicyDto.ResourceRules getResourceRules(String tierCode) {
        SysTierPolicy policy = getByTierCode(tierCode);
        if (policy == null || policy.getResourceRules() == null) {
            return new TierPolicyDto.ResourceRules();
        }
        return policy.getResourceRules();
    }
}
