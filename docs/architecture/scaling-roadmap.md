# 声读 SoundRead — 高并发 & 大数据量演进方案

> 面试准备：当被问到"数据量大了 / 并发高了怎么办"时，按层级展开回答。
> 每层都结合项目现有架构说明，面试官能感受到你**真正思考过**，而非背八股。

---

## 当前架构瓶颈分析

| 瓶颈点 | 现状 | 风险场景 |
|--------|------|----------|
| **MySQL 单库** | 所有表在一个库，单机读写 | 用户量 > 10W 后查询变慢，连接池耗尽 |
| **Redis 单节点** | 配额计数、缓存都在一个 Redis | 宕机导致配额失控、缓存雪崩 |
| **TTS 同步阻塞** | v1.0 短文本走 HTTP 同步，长文本虽有 RocketMQ 但消费者单机 | 100 人同时合成就可能排队 |
| **AI 调用串行** | LLM 生成是阻塞式的，每请求占一个线程 | DeepSeek API 慢时线程池被占满 |
| **单机部署** | Spring Boot 单实例 | 任何宕机 = 全站不可用 |

---

## 第一层：热路径缓存优化（0 成本，立即可做）

### 1.1 作品列表 + 发现页缓存

```
现状：每次请求 → 查 MySQL（JOIN + 热度排序）
优化：Redis ZSET 缓存热度排行

// 发布/互动时更新
ZINCRBY discover:hot {workId} {score}

// 首页请求
ZREVRANGE discover:hot 0 19 WITHSCORES  ← 微秒级
```

- 设置 TTL 5 分钟，定时任务兜底刷新
- 减少 90% 的 Discover 页 MySQL 查询

### 1.2 用户策略缓存（已有，可强化）

```
现状：L1 JVM Map + L2 Redis Hash（已实现）
强化：
- L1 加 Caffeine 替代 ConcurrentHashMap，自带过期淘汰
- 多实例部署时 L1 不一致问题：用 Redis Pub/Sub 广播失效通知
```

### 1.3 配额限流预检

```
现状：每次请求都 Redis INCRBY
优化：本地预扣 + 批量同步

// 本地维护一个 AtomicInteger 预扣池
// 每 5 秒批量 INCRBY 同步到 Redis
// 减少 80% 的 Redis 调用
```

> **面试话术**："当前配额用 Redis INCRBY 原子操作保证并发安全，如果 QPS 再高，可以在本地维护预扣池，批量同步到 Redis，减少网络往返。"

---

## 第二层：MySQL 读写分离 + 分表（用户 > 10W）

### 2.1 读写分离

```
                    ┌─── 从库 1（读）← 发现页/作品列表/历史记录
写入 → MySQL 主库 ──┤
                    └─── 从库 2（读）← 配额查询/用户信息

实现：ShardingSphere-JDBC / MyBatis 多数据源
```

- 写操作（创作/发布/配额扣减）走主库
- 读操作（列表/详情/配额展示）走从库
- 配置简单：Spring Boot 多数据源 + `@DS("slave")` 注解

### 2.2 大表水平分表

```
最先膨胀的表：
- studio_section（每个项目 × 每章 = 海量数据）
- music_task（每次生成一条）
- novel_segment（小说逐段）

分表策略：按 user_id 哈希取模
studio_section_0 ~ studio_section_15（16 张表）

工具：ShardingSphere 分片规则
```

> **面试话术**："studio_section 是增长最快的表，每个用户每章都有一条记录。当数据量过千万时，按 user_id 做哈希分表，保证同一用户的数据在同一张表，查询不跨分片。"

---

## 第三层：RocketMQ 异步解耦（并发 > 500 QPS）

### 3.1 TTS 合成全面异步化

```
现状：v1.0 短文本 → 同步 HTTP 返回
优化：全部走 MQ

Producer（Controller）          Consumer（TtsWorker）
     │                              │
     │── MQ: tts-task ──────────────▶│── 调用 TTS API
     │                              │── 上传 R2
     │◀── MQ: tts-result ──────────│── 更新 DB
     │
     └── SSE/WebSocket 推送前端
```

- **削峰**：100 人同时提交，MQ 缓冲，Consumer 按速率消费
- **重试**：TTS API 超时/失败，MQ 自动重试 3 次
- **横向扩展**：Consumer 可部署多实例，并行消费

### 3.2 AI 音乐生成异步优化

```
现状：@Scheduled 每 5s 轮询 Mureka
优化：
- 提交时发 MQ 消息，延迟消费做轮询
- 每个任务独立 delay message（30s/60s/120s 递增）
- 避免定时任务扫全表的 O(n) 开销
```

### 3.3 小说创作异步 Pipeline

```
现状：SSE 流式生成（已异步）
优化：逐章生成改为 MQ 驱动的 Pipeline

topic: novel-chapter-generate
  → Consumer: 生成正文
  → topic: novel-chapter-summarize
    → Consumer: 生成摘要 + RAG 索引
    → topic: novel-chapter-notify
      → Consumer: WebSocket 通知前端
```

---

## 第四层：分布式任务调度（多实例部署）

### 4.1 @Scheduled 的问题

```
现状：@Scheduled pollMurekaTasks() 每 5s 跑
问题：部署 2 个实例 → 同一个任务被轮询 2 次 → 重复处理

解决方案：
1. 简单方案：Redis 分布式锁
   SETNX poll:mureka:lock {instanceId} EX 10

2. 专业方案：XXL-Job 分布式调度
   - 可视化任务管理
   - 分片广播（10000 个任务分给 N 个实例）
   - 失败重试 + 告警
```

### 4.2 热度排序定时刷新

```
现状：每次查询实时计算热度
优化：XXL-Job 每 10 分钟批量计算 → 写入 Redis ZSET
```

---

## 第五层：CDN + R2 边缘加速（音频访问量大）

### 5.1 音频 CDN 分发

```
现状：R2 公共域名直接访问
优化：Cloudflare CDN（R2 天然集成）

用户 → CF CDN 边缘节点（命中缓存）→ 直接返回
                 ↓（未命中）
              R2 源站

- 音频文件设置 Cache-Control: max-age=604800（7 天）
- 减少 R2 出站流量费用
- 全球用户就近访问
```

### 5.2 音频转码优化

```
现状：原始 MP3 直接存储
优化：
- TTS 输出 → 统一转 128kbps MP3（减小 50% 体积）
- 生成缩略音频（前 30s 试听版）
- 按需加载：列表页加载试听版，播放时加载完整版
```

---

## 第六层：服务拆分（用户 > 100W，团队 > 3 人）

### 6.1 微服务拆分方向

```
当前单体                     拆分后
┌─────────────┐         ┌──────────┐
│ soundread   │         │ gateway  │ ← Nginx / Spring Cloud Gateway
│   - auth    │         ├──────────┤
│   - tts     │    →    │ auth-svc │ ← 用户/登录/配额
│   - studio  │         │ tts-svc  │ ← TTS 合成（CPU/IO 密集）
│   - music   │         │ ai-svc   │ ← AI 创作/小说/音乐（GPU/API 密集）
│   - novel   │         │ cms-svc  │ ← 内容管理/发布/审核
│   - admin   │         └──────────┘
└─────────────┘
```

### 6.2 拆分优先级

| 优先级 | 服务 | 理由 |
|--------|------|------|
| P0 | tts-svc | IO 密集，独立扩缩容，不影响其他服务 |
| P1 | ai-svc | LLM 调用慢，需要大线程池，独立隔离 |
| P2 | auth-svc | 配额/策略高频读，独立缓存策略 |
| P3 | cms-svc | 内容管理低频，最后拆 |

> **面试话术**："目前单体架构完全够用。如果要拆，我会优先拆 TTS 服务——它是 IO 密集型，调用三方 API 响应慢，独立部署后可以横向扩容消费者，不会因为 TTS 排队影响到创作和浏览体验。"

---

## 面试应答策略

当面试官问："你这个项目并发量多少？数据量大了怎么办？"

### 回答模板

> "这是个人项目，目前用户量不大，但架构设计时**预留了扩展空间**：
>
> **第一步**，我已经做了两级缓存（L1 JVM + L2 Redis）和 Redis 原子配额控制，热数据不打 DB；
>
> **第二步**，如果并发上来，TTS 和 AI 音乐这种耗时操作可以全面走 RocketMQ 异步化——项目里 RocketMQ 依赖已经引入，长文本 TTS 已经是异步的；
>
> **第三步**，MySQL 读写分离 + studio_section 等大表按 user_id 分表；
>
> **第四步**，@Scheduled 定时任务迁移到 XXL-Job 支持多实例分片；
>
> **第五步**，利用 Cloudflare CDN 加速音频分发，R2 天然支持；
>
> **最终**，如果团队扩大，优先把 TTS 服务和 AI 服务拆出来独立部署，它们是 IO/API 密集型，和主站解耦后互不影响。"
>
> 这样**六步递进**，每一步都有具体技术方案，而且和我现有代码能对应上。
