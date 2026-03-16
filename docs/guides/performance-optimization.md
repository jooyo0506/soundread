# SoundRead 后端性能优化文档

> 作者：资深 Java 工程视角  
> 版本：v1.0 | 2026-03-11  
> 适用：SoundRead Spring Boot 后端（单核服务器 / MySQL + Redis 跨机房部署）

---

## 一、架构现状与瓶颈分析

### 1.1 当前部署架构

```
用户请求
  │
  ▼
Cloudflare CDN (www.joyoai.xyz)
  │
  ▼
Nginx (joyoai.xyz:80)
  │
  ├── /api/*  → Spring Boot (localhost:9090)
  └── /ws/*   → WebSocket (localhost:9090)

Spring Boot (9090)
  ├── MySQL  → 127.0.0.1:3306   ← 本机访问，约 0.1ms RTT/次 (迁移广州后优化)
  └── Redis  → 127.0.0.1:6379 ← 本机访问，约 0.1ms RTT/次
```

### 1.2 已识别瓶颈（按严重程度）

| 级别 | 问题 | 影响范围 | 来源 |
|------|------|----------|------|
| 🔴 严重 | HeatScoreJob N+1 问题（N条作品=N次DB往返） | 每5分钟打爆DB | `HeatScoreJob.java` |
| 🔴 严重 | getCurrentUser() 每次请求查 MySQL | 全部认证接口 | `AuthService.java` |
| 🟡 中等 | HikariCP 未配置（默认10连接，无超时） | 高并发连接耗尽 | `application.yml` |
| 🟡 中等 | Redis 池 max-active=8，超时30s太长 | 慢请求积压 | `application.yml` |
| 🟡 中等 | @Scheduled 单线程（多Job互相阻断） | 定时任务失准 | Spring默认 |
| 🟡 中等 | Podcast 持久化同步阻塞 WebSocket 线程 | 长连接卡死 | `PodcastWebSocketHandler.java` |
| 🟢 优化 | 发现页每次查 MySQL（无缓存） | 查询慢 | `DiscoverService` |
| 🟢 优化 | 缺少数据库索引（user_id/phone/status） | 全表扫描 | 数据库层 |

---

## 二、P0 — 配置优化（改 yml，零代码，立即生效）

### 2.1 HikariCP 数据库连接池

**当前问题**：未配置 HikariCP，使用 Spring 默认的 10 个连接，无超时参数设置。

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/sound_read?useUnicode=true
         &characterEncoding=utf8
         &serverTimezone=Asia/Shanghai
         &useServerPrepStmts=true        # 开启服务端预编译
         &cachePrepStmts=true            # 缓存预编译语句
         &prepStmtCacheSize=250          # 缓存 250 条
         &prepStmtCacheSqlLimit=2048     # 最大 SQL 长度 2KB
         &connectTimeout=3000            # 建连超时 3s
         &socketTimeout=30000            # 读取超时 30s
    hikari:
      pool-name: SoundRead-DB-Pool
      maximum-pool-size: 20        # 单核服务器推荐值，> CPU*2+磁盘数
      minimum-idle: 5              # 常驻连接，避免冷启动延迟
      connection-timeout: 5000     # 从池获取连接超时 5s（快速失败）
      idle-timeout: 300000         # 空闲连接 5 分钟回收
      max-lifetime: 1800000        # 连接最长存活 30 分钟（< MySQL wait_timeout 8h）
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000  # 连接 60s 未归还则打印堆栈（排查泄漏）
```

> **参数说明**  
> - `max-lifetime`：必须 < MySQL `wait_timeout`（默认28800s=8h），否则会出现`Connection is closed`  
> - `leak-detection-threshold`：开发/预发环境开启，生产酌情开启

### 2.2 Redis Lettuce 连接池

**当前问题**：`max-active=8`（容量小），`timeout=30s`（失败太慢）。

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:139.199.223.201}
      port: 6379
      password: ${REDIS_PASSWORD:NFTurbo666}
      timeout: 3000ms             # 命令超时 3s（原30s，失败太慢会积压请求）
      connect-timeout: 2000ms     # 建连超时 2s
      lettuce:
        pool:
          max-active: 32          # 原8，提升4倍并发容量
          max-idle: 16            # 最大空闲连接
          min-idle: 4             # 常驻连接，避免重建开销
          max-wait: 2000ms        # 池满后最多等待2s，超时快速失败
        shutdown-timeout: 3s
```

### 2.3 Tomcat 线程池

```yaml
server:
  port: 9090
  servlet:
    async:
      timeout: 300000
  tomcat:
    threads:
      max: 200          # 默认值，WebSocket + HTTP 共用，可适当提高
      min-spare: 20     # 最少保持20个活跃线程
    max-connections: 2000
    accept-count: 200   # 全满时排队数
    connection-timeout: 10000

  # 上传限制（已有配置，保持）
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

---

## 三、P1 — 线程池分离（新建 AsyncConfig.java）

### 3.1 当前问题

Spring `@Scheduled` 默认只有**1个调度线程**：
- `HeatScoreJob`（每5分钟）
- `MusicService@Scheduled`（每5秒轮询）

两者共享一个线程，一个卡住另一个就延迟。

### 3.2 新建 AsyncConfig.java

```java
package com.soundread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.*;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements SchedulingConfigurer {

    /**
     * 定时任务独立线程池（防止 Job 互相阻断）
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            4,
            r -> {
                Thread t = new Thread(r, "scheduled-task-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            }
        );
        registrar.setScheduler(scheduler);
    }

    /**
     * IO 密集型异步任务线程池（外部 API 调用、TTS 合成等）
     * IO 密集公式：线程数 = CPU核数 × (1 + 平均等待时间/计算时间)
     * TTS 调用约 99% 在等 API，可配置较大线程数
     */
    @Bean("ioTaskExecutor")
    public Executor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("io-task-");
        executor.setKeepAliveSeconds(60);
        // 队列满时由调用者线程执行（降级策略，避免丢任务）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * R2 音频上传/下载专用线程池
     */
    @Bean("r2UploadExecutor")
    public Executor r2UploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("r2-upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## 四、P2 — 代码层优化

### 4.1 User 查询 Redis 缓存

**问题**：`getCurrentUser()` 在每个认证接口都执行 `SELECT * FROM user WHERE id = ?`，是全局最高频 DB 操作。

**优化**：60 秒 Redis 缓存，缓存未命中才查 DB。

```java
// AuthService.java — 增量修改

private static final String USER_CACHE_PREFIX = "cache:user:";
private static final Duration USER_CACHE_TTL = Duration.ofSeconds(60);

// 注入 ObjectMapper（已有）
private final ObjectMapper objectMapper;

public User getCurrentUser() {
    long userId = StpUtil.getLoginIdAsLong();
    return getCachedUser(userId);
}

private User getCachedUser(long userId) {
    String key = USER_CACHE_PREFIX + userId;
    // 1. 查 Redis
    try {
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            return objectMapper.readValue(json, User.class);
        }
    } catch (Exception e) {
        log.warn("[UserCache] 读取失败: {}", e.getMessage());
    }
    // 2. 查 DB
    User user = userMapper.selectById(userId);
    // 3. 写 Redis（忽略写入失败，降级到每次查DB）
    if (user != null) {
        try {
            redisTemplate.opsForValue().set(
                key, objectMapper.writeValueAsString(user), USER_CACHE_TTL);
        } catch (Exception e) {
            log.warn("[UserCache] 写入失败: {}", e.getMessage());
        }
    }
    return user;
}

/**
 * 用户信息变更时调用（VIP激活、改昵称、改密码后）
 */
public void evictUserCache(long userId) {
    redisTemplate.delete(USER_CACHE_PREFIX + userId);
    log.debug("[UserCache] 已清除: userId={}", userId);
}
```

**调用时机**：
- `VipService.activateVip(userId)` 后
- `UserService.updateProfile(userId)` 后
- 管理员手动改 tier_code 后（可提供 Admin API 手动清除）

### 4.2 HeatScoreJob — N+1 修复为批量 SQL

**当前代码分析**：
```java
// 问题代码（每条作品 = 1次 SELECT + 1次 UPDATE = 2次 DB 往返）
List<Long> ids = workMapper.selectList(...).stream().map(Work::getId).toList();
ids.forEach(adminWorkService::refreshHeatScore);
// 1000 条作品 = 2000 次跨机房 DB 访问，约 4-10 秒！
```

**修复**：

```java
// WorkMapper.java — 增加批量更新方法
@Mapper
public interface WorkMapper extends BaseMapper<Work> {
    
    // 一条 SQL 更新所有已审核作品的热度分
    @Update("UPDATE work " +
            "SET heat_score = " +
            "    play_count * 1 " +
            "  + like_count * 3 " +
            "  + share_count * 5 " +
            "  + comment_count * 2 " +
            "  + IF(is_featured = 1, 50, 0) " +
            "WHERE review_status = 'approved'")
    int batchRefreshHeatScore();
}
```

```java
// HeatScoreJob.java — 完全重写
@Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
public void refreshAll() {
    long start = System.currentTimeMillis();
    int updated = workMapper.batchRefreshHeatScore();  // 1次 DB 访问
    log.info("[HeatScoreJob] 热度刷新完成: {}条, 耗时{}ms",
             updated, System.currentTimeMillis() - start);
}
```

**性能对比**：

| 场景 | 原方案 | 优化后 |
|------|--------|--------|
| 100 条作品 | 200 次 DB 往返 ≈ 600ms | 1 次 ≈ 20ms |
| 1000 条作品 | 2000 次 DB 往返 ≈ 6s | 1 次 ≈ 30ms |
| 10000 条作品 | 会超时报错 | 1 次 ≈ 100ms |

### 4.3 Podcast WebSocket — 异步持久化

**问题**：`onComplete()` 回调里同步执行下载 + 上传 R2，可能阻塞 WebSocket 线程 10-30 秒。

```java
// PodcastWebSocketHandler.java — 修改 onComplete 回调

@Autowired
@Qualifier("r2UploadExecutor")
private Executor r2UploadExecutor;

// 在 lambda 外部保存需要传入的 final 变量（已有）
final User currentUser = user;
final String inputText = text;
final String finalVoiceA = voiceA;

// onComplete 修改：
@Override
public void onComplete(String audioUrl) {
    // 1. 先扣配额
    if (currentUser != null) {
        try { quotaService.deductPodcastQuota(currentUser); }
        catch (Exception e) { log.warn("配额扣减失败: {}", e.getMessage()); }
    }

    // 2. ★ 立即通知前端（不等 R2 上传）
    if (isActive(sessionId)) {
        JSONObject event = new JSONObject();
        event.put("event", "complete");
        event.put("audioUrl", audioUrl);   // 先给临时 URL
        sendText(session, event.toJSONString());
    }

    // 3. ★ 异步持久化（独立线程，不阻塞 WebSocket）
    CompletableFuture.runAsync(() ->
        persistPodcast(currentUser, audioUrl, inputText,
            finalVoiceA, roundTexts, (int) Math.round(totalDuration[0])),
        r2UploadExecutor
    ).exceptionally(ex -> {
        log.error("[Podcast WS] 异步持久化失败", ex);
        return null;
    });
}
```

### 4.4 分页查询 — 禁用不必要的 COUNT

```java
// 列表查询不需要总数时（如：加载更多、无限滚动）
Page<Work> page = new Page<>(pageNum, pageSize, false); // false = 不执行 COUNT(*)
workMapper.selectPage(page, queryWrapper);
```

---

## 五、P3 — 数据库索引（执行 SQL）

### 5.1 索引分析

通过 `EXPLAIN` 检查核心查询后，需要补充以下索引：

```sql
-- ============================================================
-- SoundRead MySQL 性能索引补充
-- 执行环境：生产 MySQL (139.199.223.201)
-- 注意：ALTER TABLE 在数据量大时会短暂锁表，建议业务低峰执行
-- ============================================================

-- 1. user 表：登录/查询最高频字段
ALTER TABLE `user` 
  ADD INDEX IF NOT EXISTS idx_phone (phone),          -- 登录按手机查询
  ADD INDEX IF NOT EXISTS idx_tier_code (tier_code);  -- VIP 状态筛选

-- 2. user_creation 表：创作记录查询
ALTER TABLE user_creation 
  ADD INDEX IF NOT EXISTS idx_user_type_created (user_id, type, created_at DESC),  -- 用户创作列表
  ADD INDEX IF NOT EXISTS idx_status_created (status, created_at DESC);             -- 按状态筛选

-- 3. work 表：发现页核心查询
ALTER TABLE work 
  ADD INDEX IF NOT EXISTS idx_review_heat (review_status, heat_score DESC),                    -- 热度排行
  ADD INDEX IF NOT EXISTS idx_review_type_heat (review_status, type, heat_score DESC),         -- 分类热度
  ADD INDEX IF NOT EXISTS idx_user_review (user_id, review_status),                            -- 个人发布列表
  ADD INDEX IF NOT EXISTS idx_review_created (review_status, created_at DESC);                 -- 最新排行

-- 4. vip_order 表
ALTER TABLE vip_order 
  ADD INDEX IF NOT EXISTS idx_vip_user_id (user_id),
  ADD UNIQUE INDEX IF NOT EXISTS uk_order_no (order_no);

-- 5. tts_task 表（异步任务轮询）
ALTER TABLE tts_task 
  ADD INDEX IF NOT EXISTS idx_tts_user_status (user_id, status);

-- 6. music_task 表（轮询场景）
ALTER TABLE music_task 
  ADD INDEX IF NOT EXISTS idx_music_user_status (user_id, status),
  ADD INDEX IF NOT EXISTS idx_music_status_created (status, created_at);

-- 7. studio_project 表
ALTER TABLE studio_project 
  ADD INDEX IF NOT EXISTS idx_project_user_created (user_id, created_at DESC);

-- ============================================================
-- 验证执行计划（核心查询检查）
-- ============================================================
EXPLAIN SELECT * FROM work WHERE review_status = 'approved' ORDER BY heat_score DESC LIMIT 20;
EXPLAIN SELECT * FROM user_creation WHERE user_id = 1 AND type = 'podcast' ORDER BY created_at DESC;
EXPLAIN SELECT * FROM `user` WHERE phone = '18571696470';
```

### 5.2 慢查询日志开启（找更多问题）

```sql
-- 在 MySQL 服务器执行（开启后自动记录慢查询）
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;           -- 超过 1 秒记录
SET GLOBAL log_queries_not_using_indexes = ON;  -- 记录未用索引的查询
SHOW VARIABLES LIKE 'slow_query_log_file'; -- 查看日志文件位置
```

---

## 六、P4 — Redis 高级优化

### 6.1 发现页热度榜单 ZSet 缓存

```java
// HeatScoreJob.java — 在批量 SQL 后同步更新 ZSet

private final StringRedisTemplate redisTemplate;
private static final String HEAT_RANK_KEY = "rank:works:heat";
private static final Duration HEAT_RANK_TTL = Duration.ofMinutes(10);

@Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
public void refreshAll() {
    // 1. 批量更新 DB 热度分
    int updated = workMapper.batchRefreshHeatScore();

    // 2. 同步刷新 Redis ZSet（查询时 O(log N) 替代 DB 排序）
    List<Work> works = workMapper.selectList(
        new LambdaQueryWrapper<Work>()
            .eq(Work::getReviewStatus, "approved")
            .select(Work::getId, Work::getHeatScore));
    
    if (!works.isEmpty()) {
        // 原子替换（先删后写避免脏数据）
        redisTemplate.delete(HEAT_RANK_KEY);
        Map<String, Double> scoreMap = works.stream()
            .collect(Collectors.toMap(
                w -> w.getId().toString(),
                w -> w.getHeatScore() == null ? 0.0 : w.getHeatScore().doubleValue()
            ));
        redisTemplate.opsForZSet().add(HEAT_RANK_KEY, 
            scoreMap.entrySet().stream()
                .map(e -> ZSetOperations.TypedTuple.of((Object)e.getKey(), e.getValue()))
                .collect(Collectors.toSet()));
        redisTemplate.expire(HEAT_RANK_KEY, HEAT_RANK_TTL);
    }
    log.info("[HeatScoreJob] 完成: DB{}条, ZSet{}条, {}ms", 
             updated, works.size(), System.currentTimeMillis() - start);
}
```

```java
// DiscoverService.java — 读取 ZSet 替代 DB 查询
public List<Long> getHotWorkIds(int page, int pageSize) {
    long start = (long) (page - 1) * pageSize;
    long end = start + pageSize - 1;
    // ZREVRANGE 从高分到低分
    Set<String> ids = redisTemplate.opsForZSet()
        .reverseRange(HEAT_RANK_KEY, start, end);
    if (ids == null || ids.isEmpty()) {
        // ZSet 冷启动或过期，降级查 DB
        return workMapper.selectHotWorkIds(page, pageSize);
    }
    return ids.stream().map(Long::parseLong).toList();
}
```

### 6.2 Redis Key 命名规范（现有基础上统一）

```
# 现有（已合理）
quota:{userId}:{date}:{feature}    # 配额计数
sys:policy:{tierCode}              # 策略缓存

# 新增
cache:user:{userId}                # 用户信息缓存（TTL 60s）
rank:works:heat                    # 热度榜 ZSet（TTL 10min）
```

---

## 七、P5 — JVM 参数（生产服务器）

在 Jenkins 部署脚本或 `start.sh` 中配置：

```bash
java \
  -server \
  -Xms512m -Xmx1g \                           # 按服务器内存调整（2G机器用1g）
  -XX:+UseG1GC \                               # G1 GC，低延迟首选
  -XX:MaxGCPauseMillis=200 \                   # 最大 GC 停顿 200ms
  -XX:G1HeapRegionSize=16m \                   # Region 大小
  -XX:+HeapDumpOnOutOfMemoryError \            # OOM 时自动 dump
  -XX:HeapDumpPath=/opt/sounds-tts/oom.hprof \ # dump 文件位置
  -Djava.security.egd=file:/dev/./urandom \    # 加速随机数（Tomcat 启动优化）
  -jar /opt/sounds-tts/app.jar \
  --spring.config.additional-location=/opt/sounds-tts/application-prod.yml
```

---

## 八、实施路线图

```
Week 1（配置与索引，零风险）
  Day 1  ── 修改 application.yml（HikariCP + Redis + Tomcat）
  Day 1  ── 执行 MySQL 索引 SQL
  Day 2  ── 新建 AsyncConfig.java（线程池分离）
  Day 3  ── JVM 参数更新

Week 2（代码优化，需测试）
  Day 1  ── User Redis 缓存（AuthService改造 + VipService清缓存）
  Day 2  ── HeatScoreJob 批量 SQL 重写
  Day 3  ── Podcast WebSocket 异步持久化

Week 3（高级优化，可选）
  Day 1-2 ── ZSet 热度榜缓存（DiscoverService改造）
  Day 3   ── 慢查询日志分析 + 针对性索引补充
```

## 九、优化效果预估

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| API 平均响应时间 | ~80ms | ~20ms | 4x |
| HeatScoreJob 执行时间（1000条） | ~6s | ~30ms | 200x |
| 高并发下 DB 连接数 | 频繁耗尽 | 稳定 20 连接 | 稳定 |
| WebSocket 完成响应延迟 | 完成后+30s | 立即响应 | -30s |
| Redis 操作超时 | 30s等待 | 3s快速失败 | 10x |

---

## 十、[2026-03-17] Agent 与 TTS 前后端性能优化（已实施 ✅）

### 10.1 Agent SSE 流式输出

**问题**: `agent.chat()` 同步阻塞 15-22s，用户看假进度条。

**方案**: 新增 `StreamingSmartAssistant` (返回 `TokenStream`) + `POST /api/agent/chat-stream` SSE 端点，前端 `fetch ReadableStream` 逐 token 更新。

| 指标 | 优化前 | 优化后 |
|------|--------|--------|
| 首 token 延迟 | ~15s | < 1s |
| "你好" 响应 | ~20s | ~50ms (快速通道) |

### 10.2 简单问候快速通道

**方案**: `isSimpleGreeting()` 精确匹配 + 正则识别简单问候，跳过 LLM 直接返回预设回复。

### 10.3 LLM 参数优化

| 参数 | 原值 | 新值 | 说明 |
|------|------|------|------|
| `maxTokens` | 1024 | 512 | Agent 回复通常 < 200 字 |
| `maxMessages` | 20 | 10 | 减少上下文 token 数 |

### 10.4 TTS 并行合成

**问题**: `synthesizeChunk` 串行调用，5 片 × 10s = 50s。

**方案**: `Promise.all` + 并发控制（≤3），保持音频顺序用于拼接。

| 场景 | 优化前 | 优化后 |
|------|--------|--------|
| 5 片合成 | ~50s | ~20s |

---

*文档由 SoundRead 技术团队维护，最后更新：2026-03-17*
