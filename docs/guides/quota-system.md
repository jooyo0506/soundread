# 配额系统规范

> 最后更新：2026-03-14  
> 适用版本：V10 及以上

## 设计原则

1. **按调用成本分层**：不同功能消耗的 API 成本差距大，单独计量高成本模块
2. **字数 vs 次数**：继续消耗型（TTS 合成）按字数，生成型（LLM 剧本创作）按次数
3. **不双重计费**：同一操作链路只在最关键的节点扣一次配额
4. **独立限额高成本入口**：AI 播客（双倍 TTS）、小说创作（超长内容）单独设限额

---

## 配额字段说明

| 字段 (`quota_limits` JSON key) | 类型 | 说明 |
|-------------------------------|------|------|
| `tts_daily_chars` | 字数/天 | TTS 1.0 基础合成（普通文本转语音） |
| `tts_v2_daily_chars` | 字数/天 | TTS 2.0 情感语音合成，覆盖所有情感配音入口 |
| `ai_script_daily_count` | 次数/天 | AI 工作台内容生成次数（见下方覆盖范围） |
| `podcast_daily_count` | 次数/天 | AI 双人播客生成（独立，因双倍 TTS 成本） |
| `novel_daily_chars` | 字数/天 | AI 小说正文生成字数（独立，消耗最高） |
| `novel_max_projects` | 个数 | 小说项目总数上限 |
| `music_daily_count` | 次数/天 | AI 音乐生成次数 |
| `storage_max_mb` | MB | 云存储空间上限 |
| `max_projects` | 个数 | 工作台项目总数上限 |
| `data_retention_days` | 天，-1=永久 | 文件保留期限 |
| `ask_daily_count` | 次数/天 | 已下线，字段保留，值设为 -1 |

---

## 各入口配额归属

### TTS 合成入口（字数）

| 入口 | 接口 | 扣减字段 |
|------|------|---------|
| 快速配音页 基础合成 | `POST /tts/synthesize` | `tts_daily_chars` |
| 快速配音页 情感合成 | `POST /tts/v2/synthesize` | `tts_v2_daily_chars` |
| Studio 段落合成音频 | `POST /tts/v2/synthesize` | `tts_v2_daily_chars` |
| 广播剧多角色合成 | `POST /tts/v2/synthesize` | `tts_v2_daily_chars` |

### AI 创作入口（次数）

| 入口 | 接口 | 扣减字段 | 说明 |
|------|------|---------|------|
| Studio 工作台内容生成 | `POST /studio/projects/{id}/generate` | `ai_script_daily_count` | 含有声绘本/电台/讲解/文案/新闻/广播剧 |
| Studio 广播剧一键生成 | `POST /studio/projects/{id}/drama-generate` | `ai_script_daily_count` | — |
| 快速配音 AI 编排 Tag | `POST /tts/ai-script` | ❌ 不计次数 | 后续合成走 `tts_v2_daily_chars` |
| 快速配音 AI 标签增强 | `POST /tts/v2/enhance-tags` | ❌ 不计次数 | 同上 |
| 快速配音 AI 场景台本 | `POST /tts/v2/generate-scene` | ❌ 不计次数 | 同上 |
| AI 双人播客 | `POST /podcast/generate` | `podcast_daily_count` | 双倍 TTS，独立控制 |
| AI 小说章节生成 | `POST /studio/projects/{id}/generate` (chapterIndex≠null) | `ai_script_daily_count` | 与工作台共用计数 |
| AI 音乐生成 | `POST /music/generate` | `music_daily_count` | — |

> **关键设计决策**：快速配音的 AI 辅助功能（AI 编排 / 标签增强 / 场景台本）**不扣次数**，
> 因为后续 TTS 合成时 `tts_v2_daily_chars` 已实质计费，双重扣费对用户不公平。

---

## 各等级配额数值（V10 版本）

| 配额项 | 免费版 (user) | VIP 版 (vip_*) |
|--------|-------------|--------------|
| TTS 基础每日字数 | 100字 | 2000字 |
| 情感语音每日字数 | 0（不可用） | 3000字 |
| AI 工作台每日次数 | 3次 | 30次 |
| AI 双人播客每日次数 | 0（不可用） | 5次 |
| AI 小说每日字数 | 0（不可用） | 5000字 |
| AI 音乐每日次数 | 0（不可用） | 10次 |
| 项目保存数量 | 5个 | 50个 |
| 云存储空间 | 50 MB | 500 MB |
| 文件保留期限 | 7天 | 永久 |

---

## 配额执行位置（后端）

```
QuotaService.checkAndDeductXxx()
    ↑ 调用方
    ├── TtsController          → tts_daily_chars
    ├── TtsV2Controller        → tts_v2_daily_chars
    ├── StudioController       → ai_script_daily_count  ← V10 新增
    ├── PodcastController      → podcast_daily_count
    ├── MusicController        → music_daily_count
    └── NovelController (TTS)  → novel_daily_chars
```

---

## 更新配额的操作流程

1. 修改 `docs/sql/migration/V{N}__update_xxx_quotas.sql`
2. 在目标数据库执行该 SQL
3. 调用 `POST /api/admin/tier-policy/refresh` 刷新内存缓存
4. 同步更新 `web/src/views/Vip.vue` 展示数字

---

## 配额降级策略（2026-03-14 更新）

### `QuotaService.getQuotaLimit` 容错处理

当未找到用户所属的策略（`TierPolicy` 缺失）时，`getQuotaLimit` 的返回值决定了功能是"默认放行"还是"默认阻断"。

```java
// 修复前：返回 0 → 用户被完全阻止所有功能
if (policy == null) return 0;

// 修复后：返回 -1 → 视为无限制，避免因配置缺失导致线上功能不可用
if (policy == null) return -1;
```

**设计决策**：`-1` 代表无限制（与 `data_retention_days: -1` 语义一致）。在 MVP 阶段优先保证可用性，策略缺失不应当成权限拒绝处理。

### SSE 流式端点的超时配置

SSE 端点不走 axios，需要在 `fetch()` 层面或通过 `AbortController` 单独管理超时。广播剧分角色配音中逐句调用的 TTS v2 API 需设置 60s 超时（默认 10s 不够）。

