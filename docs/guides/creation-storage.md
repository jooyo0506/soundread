# 10 — 创作历史存储方案选型与实现

> 版本: v1.0 | 日期: 2026-02-27

---

## 一、需求背景

用户每次 TTS 合成（短文本/情感/导演/播客/有声书）完成后，需要：
1. **保存录音** — 用户随时可回看、重播
2. **历史记录** — "我的创作"页面展示完整创作历史
3. **存储计量** — 每个用户有存储配额（免费 50MB / VIP 无限）
4. **发布流转** — 优质创作可发布到"发现页"

---

## 二、存储架构方案选型

### 2.1 方案对比

| 方案 | 核心思想 | 查余量 | 并发安全 | 删除释放 | 数据一致性 |
|:-----|:---------|:-------|:---------|:---------|:-----------|
| **① 单表聚合** | 只用 `user_creation`，`SUM(file_size)` 算余量 | ❌ O(N) | ❌ 竞态 | ⚠️ 需重算 | ✅ 天然一致 |
| **② 双表分离** | `user_creation`（明细）+ `user_storage`（计量） | ✅ O(1) | ✅ 原子更新 | ✅ 递减即可 | ⚠️ 需事务保证 |
| **③ Redis 缓存** | 计量存 Redis，明细存 DB | ✅ O(1) | ✅ INCR 原子 | ✅ DECR | ❌ 宕机丢数据 |

### 2.2 详细分析

#### 方案 ① 单表聚合（不推荐）

```sql
-- 每次检查配额都要聚合扫描
SELECT SUM(file_size) FROM user_creation WHERE user_id = ? AND deleted = 0
```

- **优点**：实现最简单，天然数据一致（只有一个数据源）
- **致命缺点**：
  - 配额校验是**高频操作**（每次合成前都要查），`SUM()` 随创作量线性增长
  - 100 个创作 → SUM 约 5ms；10000 个创作 → SUM 约 50ms
  - 并发合成时，两个请求同时 SUM 可能读到相同旧值，导致超额
- **适用**：极小规模 Demo

#### 方案 ② 双表分离（✅ 采用）

```sql
-- 查余量: O(1)，命中主键索引
SELECT used_bytes FROM user_storage WHERE user_id = ?

-- 写入: 同一事务内原子完成
BEGIN;
  INSERT INTO user_creation (...) VALUES (...);
  UPDATE user_storage SET used_bytes = used_bytes + ?, file_count = file_count + 1 WHERE user_id = ?;
COMMIT;
```

- **优点**：
  - 配额检查永远 O(1)，与创作量无关
  - `used_bytes = used_bytes + ?` 行锁原子更新，并发安全
  - 删除时 `used_bytes = used_bytes - ?` 即时释放
  - `user_storage` 永远只有用户数那么多行，不膨胀
- **缺点**：需要在同一事务中保证两表一致
- **类比**：银行的"交易流水表" + "账户余额表"

#### 方案 ③ Redis 缓存计量（不推荐）

```java
redisTemplate.opsForValue().increment("storage:" + userId, fileSize);
```

- **优点**：极致性能（内存操作 < 0.1ms）
- **致命缺点**：
  - Redis 宕机/重启 → 计量数据丢失，与明细表不一致
  - 需要定时对账修复（运维成本高）
- **适用**：超高并发场景（百万 QPS），但需配合持久化补偿机制

### 2.3 最终选择

> **方案 ②：双表分离（`user_creation` + `user_storage`）**
>
> 选型理由：
> 1. 配额检查 O(1)，无论用户创建多少作品，查询性能恒定
> 2. `UPDATE SET used_bytes = used_bytes + ?` 行锁原子操作，天然并发安全
> 3. 零额外依赖（纯 MySQL），不增加运维成本
> 4. 与既有的 MyBatis-Plus 技术栈无缝集成

---

## 三、数据模型设计

### 3.1 表结构

```sql
-- 创作明细表（每次合成一条）
CREATE TABLE user_creation (
  id bigint NOT NULL COMMENT '雪花ID',
  user_id bigint NOT NULL,
  type varchar(20) NOT NULL COMMENT 'tts/emotion/drama/podcast/novel',
  title varchar(200) DEFAULT NULL,
  input_text text COMMENT '输入文本（回看用）',
  voice_id varchar(100) DEFAULT NULL COMMENT '音色ID',
  audio_url varchar(500) NOT NULL COMMENT '音频URL(R2)',
  audio_duration int DEFAULT 0 COMMENT '时长(秒)',
  file_size bigint DEFAULT 0 COMMENT '文件大小(字节)',
  is_published tinyint DEFAULT 0,
  work_id bigint DEFAULT NULL COMMENT '发布后关联的work.id',
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_type (user_id, type),
  KEY idx_user_created (user_id, created_at DESC)
);

-- 存储计量表（每用户一行）
CREATE TABLE user_storage (
  user_id bigint NOT NULL,
  used_bytes bigint DEFAULT 0 COMMENT '已用字节',
  file_count int DEFAULT 0 COMMENT '文件数',
  quota_override_mb int DEFAULT NULL COMMENT '运营手动覆盖的容量上限(NULL=按策略)',
  last_calculated_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id)
);
```

### 3.2 数据流

```
合成请求 → TTS引擎合成 → R2上传音频
                              ↓
                    CreationService.save()  ← @Transactional
                        ↓                ↓
              INSERT user_creation    UPDATE user_storage
              (写入创作明细)           (递增 used_bytes)
                        ↓
                    返回结果给前端
```

### 3.3 读写路径

| 操作 | 写入 | 读取 |
|:-----|:-----|:-----|
| **合成** | `INSERT creation` + `UPDATE storage(+)` | — |
| **删除** | `DELETE creation` + `UPDATE storage(-)` | — |
| **查余量** | — | `SELECT used_bytes FROM user_storage` |
| **查历史** | — | `SELECT * FROM user_creation ORDER BY created_at DESC` |
| **发布** | `UPDATE creation` + `INSERT work` | — |

---

## 四、一致性保障

### 4.1 事务保证

```java
@Transactional(rollbackFor = Exception.class)
public UserCreation save(UserCreation creation) {
    creationMapper.insert(creation);                        // 明细
    storageQuotaService.addStorage(userId, fileSize);       // 计量
    return creation;
    // 任一步失败 → 全部回滚，两表保持一致
}

@Transactional(rollbackFor = Exception.class)
public void delete(Long creationId, Long userId) {
    creationMapper.deleteById(creationId);                  // 逻辑删除明细
    storageQuotaService.releaseStorage(userId, fileSize);   // 释放计量
}
```

### 4.2 数据修复（兜底）

极端情况下若两表不一致，可通过定时任务修复：

```sql
-- 以 user_creation 明细表为准，重算 user_storage
UPDATE user_storage us SET
  used_bytes = (SELECT COALESCE(SUM(file_size), 0) FROM user_creation WHERE user_id = us.user_id AND deleted = 0),
  file_count = (SELECT COUNT(*) FROM user_creation WHERE user_id = us.user_id AND deleted = 0),
  last_calculated_at = NOW();
```

---

## 五、面试话术

> "创作历史存储我采用了**双表分离架构**——`user_creation` 记录每条合成明细（类似银行流水），`user_storage` 记录用户存储汇总（类似账户余额）。
>
> 选这个方案而不是单表 SUM 聚合，核心原因是**配额检查频率高**——每次合成前都要校验空间是否足够。单表方案需要 `SUM(file_size)` 全表扫描，时间复杂度 O(N) 随创作量线性增长；双表方案直接 `SELECT used_bytes` 主键命中，永远 O(1)。
>
> 一致性方面，保存和删除都用 `@Transactional` 保证两表原子更新——INSERT 明细 + UPDATE 计量在同一事务中完成，任意一步失败全部回滚。极端情况下还有定时对账 SQL 兜底。
>
> 这实际上是 CQRS 思想的一个简化版——写入时多做一步增量更新，换取读取时的 O(1) 性能。绝大多数 SaaS 产品（网盘、视频平台、云存储）都采用类似架构。"
