# SoundRead 性能优化路线图

> 基于项目代码深度分析，按优先级从高到低排列。
> 每个优化项标注了**当前状态**、**优化方案**、和**建议时机**。

---

## 📊 项目现状快照

| 维度 | 当前状态 |
|---|---|
| 架构 | 单体 Spring Boot 3.2 |
| 数据库 | 单机 MySQL 8.0，21 张表 |
| 缓存 | Redis（仅用于 Sa-Token 会话 + 配额计数） |
| 消息队列 | RocketMQ（已引入，少量使用） |
| 异步 | `@EnableAsync` + 默认线程池 |
| 连接池 | HikariCP（Spring Boot 默认，未调参） |
| 索引 | 仅 3 个自定义索引（work 表 2 个，user_creation 表 1 个） |
| 限流 | ❌ 无 |
| CDN | ❌ 无（音频文件直接走 R2 公网域名） |

---

## 🔴 P0 — 立即可做（不需要等用户量）

### 1. 数据库索引补全

**问题**：多张高频查询表缺少索引。当数据量增长到万级，查询会显著变慢。

**当前索引情况**：

| 表 | 已有索引 | ❌ 缺失的索引 |
|---|---|---|
| `work` | `PRIMARY`, `idx_category_play` | `(user_id, deleted)`, `(review_status, deleted)`, `(status, deleted)`, `(source_type)` |
| `user_creation` | `idx_user_id`, `idx_user_created` | `(source_type, deleted)` |
| `user` | `PRIMARY` | `(phone)` — 登录查询必用！ |
| `studio_project` | `PRIMARY` | `(user_id, created_at)` |
| `studio_section` | `PRIMARY` | `(project_id, section_index)` |
| `music_task` | `PRIMARY` | `(user_id, status)` |
| `novel_project` | `PRIMARY` | `(user_id, deleted)` |
| `tts_task` | `PRIMARY` | `(user_id, created_at)` |

**执行 SQL**：
```sql
-- ========== P0 索引补全 ==========

-- 用户登录（每次都查，最紧急！）
ALTER TABLE user ADD INDEX idx_phone (phone);

-- 发现页：按状态+逻辑删除筛选
ALTER TABLE work ADD INDEX idx_status_deleted (status, deleted);
ALTER TABLE work ADD INDEX idx_review_deleted (review_status, deleted);
ALTER TABLE work ADD INDEX idx_userid_deleted (user_id, deleted);

-- 工作台项目列表
ALTER TABLE studio_project ADD INDEX idx_user_created (user_id, created_at DESC);
ALTER TABLE studio_section ADD INDEX idx_project_idx (project_id, section_index);

-- 音乐任务轮询
ALTER TABLE music_task ADD INDEX idx_user_status (user_id, status);

-- 小说项目
ALTER TABLE novel_project ADD INDEX idx_user_deleted (user_id, deleted);

-- 创作记录
ALTER TABLE user_creation ADD INDEX idx_source_deleted (source_type, deleted);

-- TTS 任务
ALTER TABLE tts_task ADD INDEX idx_user_created (user_id, created_at DESC);
```

> ⚠️ `user.phone` 索引是最紧急的——**每次登录都全表扫描**。用户多了以后登录会越来越慢。

---

### 2. 异步线程池配置

**问题**：`@EnableAsync` 使用的是 Spring 默认的 `SimpleAsyncTaskExecutor`，每次请求都**新建线程**，没有复用。在高并发下会导致线程爆炸。

**当前使用 @Async 的代码**：
- `VoiceCloneService.@Async` — 声音克隆
- `NovelPipelineService.@Async` — 小说生成管线

**修复方案**：创建 `config/AsyncConfig.java`

```java
package com.soundread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);          // 核心线程数
        executor.setMaxPoolSize(16);          // 最大线程数
        executor.setQueueCapacity(100);       // 等待队列容量
        executor.setKeepAliveSeconds(60);     // 空闲线程存活时间
        executor.setThreadNamePrefix("async-");
        // 队列满了之后，由调用者线程执行（不会丢任务）
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

### 3. HikariCP 连接池调参

**问题**：Spring Boot 默认 HikariCP 配置（`maximumPoolSize=10`），未根据业务负载调优。

**在 `application.yml` 添加**：
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20          # 并发连接上限
      minimum-idle: 5                # 最小空闲连接
      idle-timeout: 600000           # 空闲超时 10 分钟
      max-lifetime: 1800000          # 连接最大存活 30 分钟
      connection-timeout: 30000      # 获取连接超时 30 秒
      leak-detection-threshold: 60000 # 泄漏检测 60 秒（超过则打日志告警）
```

---

## 🟡 P1 — 用户破千时做

### 4. Redis 业务缓存

**问题**：Redis 目前只给 Sa-Token 和配额计数用。大量可缓存的高频查询每次都打数据库。

**建议缓存的接口**：

| 接口 | 当前每次都查库 | 缓存策略 | 预期效果 |
|---|---|---|---|
| 声音列表 `VoiceServiceImpl.listAll()` | ✅ 每次进工坊 | 1 小时，声音变更时清除 | 减少 80% DB 查询 |
| 发现页热门 `ContentService.getWorks("hot")` | ✅ 每次首页 | 5 分钟自动过期 | 最高频接口，收益最大 |
| AI 提示模板 `AiPromptLibraryService.list()` | ✅ 每次创作 | 30 分钟 | 数据几乎不变 |
| 等级策略 `TierPolicyService.getAll()` | 已有 Redis ✅ | — | — |
| 用户配额 `QuotaService.check()` | 已有 Redis ✅ | — | — |

**实现方式一：Spring Cache 注解**（推荐，最简单）

① `pom.xml` 已有 `spring-boot-starter-data-redis`，只需开启：
```java
// 在主启动类或配置类加上
@EnableCaching
```

② 在 Service 方法上加注解：
```java
@Cacheable(value = "voices", key = "'all'",
           unless = "#result == null || #result.isEmpty()")
public List<SysVoice> listAllVoices() {
    return sysVoiceMapper.selectList(null);
}

// 声音变更时清缓存
@CacheEvict(value = "voices", allEntries = true)
public void updateVoice(SysVoice voice) { ... }
```

③ 配置 Redis Cache TTL：
```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .withCacheConfiguration("voices",
            config.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("discover_hot",
            config.entryTtl(Duration.ofMinutes(5)))
        .build();
}
```

---

### 5. 接口限流

**问题**：所有 API 无限流保护。恶意用户或爬虫可以无限调用 TTS/AI 接口，耗尽资源和第三方 API 额度。

**高危接口**：

| 接口 | 风险 | 建议限流 |
|---|---|---|
| `/api/tts/generate` | TTS 调用极费资源 | 5 次/分/用户 |
| `/api/studio/*/ask` | AI 大模型调用 | 10 次/分/用户 |
| `/api/music/generate` | 第三方 Mureka API | 3 次/分/用户 |
| `/api/auth/login` | 防暴力破解 | 10 次/分/IP |
| `/api/novel/generate` | AI 小说生成 | 5 次/分/用户 |

**实现方案**：自定义注解 + Redis 滑动窗口

① 定义注解：
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 10;     // 最大次数
    int window() default 60;    // 时间窗口（秒）
    String key() default "";    // 限流维度（为空则按 userId）
}
```

② AOP 拦截器：
```java
@Aspect
@Component
public class RateLimitAspect {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) {
        String key = "rate:" + StpUtil.getLoginIdAsString()
                     + ":" + pjp.getSignature().getName();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, rateLimit.window(), TimeUnit.SECONDS);
        }
        if (count > rateLimit.limit()) {
            throw new BusinessException("操作太频繁，请稍后再试");
        }
        return pjp.proceed();
    }
}
```

③ 使用：
```java
@RateLimit(limit = 5, window = 60)
@PostMapping("/generate")
public Result<...> generateTts(...) { ... }
```

---

### 6. 慢查询监控

**MySQL 慢查询日志**：
```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;    -- 超过 1 秒的查询记录
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';
```

**MyBatis-Plus SQL 日志（开发环境）**：
```yaml
# application.yml 取消注释
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**定期分析**：
```bash
mysqldumpslow -s t /var/log/mysql/slow.log | head -20
```

---

## 🟢 P2 — 用户破万时做

### 7. 热点数据多级缓存（L1 + L2）

```
请求 → L1 Caffeine 本地缓存（微秒级，进程内）
         ↓ miss
      L2 Redis 分布式缓存（亚毫秒级，跨进程）
         ↓ miss
      L3 MySQL 数据库（毫秒级）
```

**适用场景**：声音列表、AI 提示模板等极少变化的数据。

项目已引入 Caffeine 依赖，可直接使用。

---

### 8. N+1 查询优化

**问题**：`AiPromptLibraryServiceImpl` 先查分类列表，再查角色列表，在内存中组装。数据量大时效率低。

```java
// 当前：2 次查询 + 内存 groupBy
List<AiPromptCategory> categories = categoryMapper.selectList(catQuery);
List<AiPromptRole> allRoles = roleMapper.selectList(roleQuery);
// 然后在内存中 forEach 组装...
```

**优化方案**：
- 方案 A：改为 JOIN 查询一次搞定
- 方案 B：整体缓存（推荐，因为这个数据几乎不变）

---

### 9. CDN 静态资源加速

**问题**：音频文件直接走 Cloudflare R2 公网域名 `r2.joyoai.xyz`。没有国内 CDN 节点，访问延迟高。

**优化方案**：
```
用户 → 国内 CDN 边缘节点（延迟 ~20ms）
         ↓ 首次回源
      Cloudflare R2 源站（延迟 ~200ms）
```

**操作步骤**：
1. 购买阿里云 CDN / 腾讯云 CDN
2. 设置源站为 `r2.joyoai.xyz`
3. 配置缓存规则：音频文件缓存 30 天
4. 前端把音频 URL 域名替换为 CDN 域名

**预期收益**：音频加载速度提升 5-10 倍。

---

### 10. 数据库读写分离

**架构**：
```
                    ┌─── 从库 1（SELECT）
写入 → 主库 MySQL ──┤
                    └─── 从库 2（SELECT）
```

**实现方案**：
- MyBatis-Plus 的 `DynamicRoutingDataSource`
- 或 Spring 的 `AbstractRoutingDataSource`
- 写操作走主库，读操作负载均衡到从库

**建议时机**：单库 QPS > 2000 或数据量 > 500 万行

---

## 🔵 P3 — 用户破十万时做

### 11. 分库分表

**候选表**：
- `work`（作品量级最大，按 `user_id` 水平分片）
- `user_creation`（用户创作记录，增长快）
- `ai_interaction`（AI 交互记录，写入频率最高）

**方案**：ShardingSphere / MyBatis-Plus 分片插件

> 💡 这时候雪花 ID 就真正发挥作用了 —— 分片后不同分片不会 ID 冲突。

---

### 12. 消息队列深度解耦

**当前**：RocketMQ 已引入但使用较少。

**建议异步化的操作**：

| 场景 | 当前方式 | 优化后 |
|---|---|---|
| TTS 合成 | 同步等待 | MQ 投递 + 轮询/WebSocket 推送结果 |
| 播放计数 | 每次直接 UPDATE | MQ 批量聚合后写入（减少写压力） |
| 热度分计算 | 定时 Job 全量刷新 | 事件驱动增量计算 |
| AI 生成完成通知 | 前端轮询 | MQ + WebSocket 实时推送 |

---

### 13. Elasticsearch 全文搜索

**问题**：作品搜索目前用 `LIKE '%keyword%'`，无法走索引，百万级数据会非常慢。

**优化**：接入 ES，支持：
- 全文检索（标题、描述、AI 摘要）
- 语义搜索（结合已有的向量能力）
- 热门推荐排序
- 实时搜索建议（输入即搜索）

---

### 14. 微服务拆分

当单体无法支撑时，按领域拆分：

```
soundread-gateway        ← API 网关 + 鉴权 + 限流
soundread-user-service   ← 用户 + VIP + 配额
soundread-content-service ← 作品 + 发现页
soundread-tts-service    ← TTS 合成（CPU密集，需独立扩缩容）
soundread-ai-service     ← AI Agent + 大模型调用
soundread-media-service  ← 音频存储 + 转码
```

> ⚠️ 微服务是**最后的手段**，引入会带来很大运维复杂度：
> 服务注册（Nacos）、链路追踪（SkyWalking）、分布式事务（Seata）、
> 配置中心、CI/CD 流水线改造等。除非团队 ≥ 5 人且各模块独立迭代，否则不建议拆。

---

## 📋 执行优先级总览

| 优先级 | 用户量级 | 优化项 | 复杂度 | 收益 |
|---|---|---|---|---|
| 🔴 P0 | 现在 | 索引补全 | ⭐ | ⭐⭐⭐⭐⭐ |
| 🔴 P0 | 现在 | 线程池配置 | ⭐ | ⭐⭐⭐ |
| 🔴 P0 | 现在 | 连接池调参 | ⭐ | ⭐⭐⭐ |
| 🟡 P1 | 破千 | Redis 业务缓存 | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| 🟡 P1 | 破千 | 接口限流 | ⭐⭐ | ⭐⭐⭐⭐ |
| 🟡 P1 | 破千 | 慢查询监控 | ⭐ | ⭐⭐⭐ |
| 🟢 P2 | 破万 | 多级缓存 | ⭐⭐⭐ | ⭐⭐⭐ |
| 🟢 P2 | 破万 | N+1 查询优化 | ⭐⭐ | ⭐⭐ |
| 🟢 P2 | 破万 | CDN 加速 | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| 🟢 P2 | 破万 | 读写分离 | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| 🔵 P3 | 破十万 | 分库分表 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 🔵 P3 | 破十万 | MQ 深度解耦 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 🔵 P3 | 破十万 | ES 搜索引擎 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 🔵 P3 | 破十万 | 微服务拆分 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

---

## 💡 黄金法则

> **不要提前优化！** 每个优化都有代价（复杂度、维护成本、调试难度）。
> 先用监控数据证明瓶颈在哪，再对症下药。

**监控三件套**（建议尽早接入）：
1. **APM**：SkyWalking / Arthas — 链路追踪，找到慢接口
2. **MySQL 慢日志** — 找到慢 SQL
3. **Prometheus + Grafana** — JVM、Redis、MQ 监控大盘
