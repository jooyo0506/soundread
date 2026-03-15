package com.soundread.common.aspect;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.soundread.common.RateLimit;
import com.soundread.common.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口限流 AOP 切面 — Caffeine 滑动窗口实现
 *
 * <h3>工作原理</h3>
 * <ol>
 * <li>按 userId + 方法名 组合为限流 key</li>
 * <li>每个 key 对应一个 AtomicInteger 计数器</li>
 * <li>Caffeine 的 expireAfterWrite 自动在窗口到期后清零</li>
 * <li>计数超限则抛出 429 BusinessException</li>
 * </ol>
 *
 * <h3>设计选择</h3>
 * <ul>
 * <li>Caffeine 而非 Redis — 单实例部署无额外依赖</li>
 * <li>按方法粒度隔离 — 不同接口有不同的限流配置</li>
 * <li>未登录用户不限流（由 SaToken 拦截器兜底拒绝）</li>
 * </ul>
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    /**
     * 每种窗口配置一个独立的 Cache 实例（避免 TTL 冲突）
     * key: windowSeconds → Cache<rateLimitKey, counter>
     */
    private final ConcurrentHashMap<Integer, Cache<String, AtomicInteger>> cachePool = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 未登录用户不走限流（由 SaToken 拦截器兜底拒绝）
        if (!StpUtil.isLogin()) {
            return joinPoint.proceed();
        }

        String userId = StpUtil.getLoginIdAsString();
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        String key = userId + ":" + methodName;

        Cache<String, AtomicInteger> cache = cachePool.computeIfAbsent(
                rateLimit.windowSeconds(),
                window -> Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(window))
                        .maximumSize(10_000)
                        .build());

        AtomicInteger counter = cache.get(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        if (current > rateLimit.maxRequests()) {
            log.warn("[RateLimit] 用户 {} 触发限流: {} ({}/{}次, 窗口{}s)",
                    userId, methodName, current, rateLimit.maxRequests(), rateLimit.windowSeconds());
            throw new BusinessException(429, rateLimit.message());
        }

        return joinPoint.proceed();
    }
}
