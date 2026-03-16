# SoundRead 变更日志

本文档记录每次代码规范整改、功能变更和重构的具体内容，按时间倒序排列。

---

## [2026-03-17] — Agent SSE 流式输出 + TTS 并行合成 + 架构修复

### 🔥 性能优化

| 优化项 | 文件 | 效果 |
|--------|------|------|
| Agent SSE 流式输出 | `StreamingSmartAssistant.java`(新), `AgentController.java`, `AiWorkshop.vue` | 首 token < 1s (vs 20s+) |
| 简单问候快速通道 | `AgentController.java` | "你好" ~50ms (跳过 LLM) |
| LLM 参数调优 | `AgentController.java` | maxTokens 1024→512, maxMessages 20→10 |
| TTS 并行合成 | `StudioWorkbench.vue` | 5 片: ~50s → ~20s (并发≤3) |

### 🐛 Bug 修复

| 问题 | 根因 | 修复 |
|------|------|------|
| 重置对话后记忆仍存在 | `reset()` 用 `token.hashCode()` 但 `chat()` 用 `user.getId()`, 且清了错误的存储 | 统一 memoryId + 调用 `sharedMemoryStore.deleteMessages()` |

### 🔧 架构

| 改动 | 文件 |
|------|------|
| 修复 WebSocketConfig 5 个编译错误 | `WebSocketConfig.java` — 移除不存在的 `InteractionWebSocketHandler` |
| TTS v2 API 统一入口 | `tts.js` — 新增 `synthesizeV2()` |
| sync/streaming Agent 共享 memoryStore | `AgentController.java` — 对话上下文在两种模式间连续 |

---

## [2026-03-15] — 游客浏览模式 + API 限流防刷 + generateLyrics 权限修复

### 🟢 游客浏览模式

| 文件 | 改动 |
|------|------|
| `SaTokenConfig.java` | 白名单新增 `/discover/works/*/play`（播放计数）、`/voice/library`（音色列表） |
| `router/index.js` | Create/Emotion/Podcast/Music/Vip 移除 `requiresAuth`，游客可浏览 UI |
| `useLoginGuard.js` | **新增** 通用游客操作拦截组合式函数 |
| `Music.vue` | `startGenerate`/`aiGenerateLyrics` 补充登录守卫 |

**设计原则**: AiWorkshop/Studio/Profile/Creations 保持需登录（空 UI 无展示价值 / Agent 成本最高）。

### 🔴 安全: API 限流防刷

| 文件 | 改动 |
|------|------|
| `RateLimit.java` | **新增** `@RateLimit` 注解 |
| `RateLimitAspect.java` | **新增** Caffeine 滑动窗口 AOP 切面（按 userId+方法维度，超限 429） |
| 5 个 Controller | 14 个 AI 端点全部打标（3/5/10 次每分钟分级） |
| `MusicController.java` | `generateLyrics` 补上 `@RequireFeature("ai_music")` |

---

## [2026-03-14] — SSE 域名修复 + 广播剧标题同步 + 配音超时修复 + Sa-Token 文档

### 🔴 紧急修复

#### SSE fetch 请求打到 Cloudflare Pages 静态域名导致 405（`studio.js`）

| 文件 | 改动 |
|------|------|
| `studio.js` | 新增 `SSE_BASE` 常量，使用 `VITE_API_BASE_URL` 替代硬编码 `/api` 前缀 |

**根因**：前端部署在 `www.joyoai.xyz`（Cloudflare Pages 静态托管），SSE 的 `fetch()` 使用相对路径 `/api/...`，浏览器自动解析为 `https://www.joyoai.xyz/api/...`。Cloudflare Pages 对 POST 请求返回 405 Method Not Allowed，后端从未被调用。而 axios 使用了 `VITE_API_BASE_URL=https://joyoai.xyz/api`（无 www），所以普通 API 不受影响。

**影响范围**：3 个 SSE 接口全部不可用：
- `generateContent`（正文生成）
- `generateDrama`（广播剧一键生成）
- `rewriteSection`（AI 改写）

---

#### @RequireFeature AOP 与 SSE 端点内容协商冲突（`StudioController.java`）

| 文件 | 改动 |
|------|------|
| `StudioController.java` | 移除 `@RequireFeature("ai_drama")`，改用内联 `tierPolicyService.hasFeature()` 检查 |
| `QuotaService.java` | `getQuotaLimit` 未找到策略时返回 `-1`（无限制）替代 `0`（阻止） |

**根因**：AOP 切面抛出的异常通过全局异常处理器转为 JSON（`application/json`），但 SSE 端点要求返回 `text/event-stream`，Spring MVC 内容协商失败导致空 405 响应。

---

### 🐛 修复

#### 广播剧分角色配音超时（`StudioWorkbench.vue` + `studio.js`）

| 接口 | 原超时 | 新超时 |
|------|--------|--------|
| `/tts/v2/synthesize`（情感 TTS） | 10s（全局默认） | 60s |
| `/studio/parse-script`（剧本解析） | 10s | 60s |
| `/studio/concat-audio`（音频拼接） | 10s | 120s |

**根因**：广播剧 32 句台词逐句调用 TTS v2 API，部分句子加情感标签后处理时间超过 10s 默认超时。

---

#### AI 音乐播放中断修复（`player.js`）

| 文件 | 改动 |
|------|------|
| `player.js` | AI 音乐生成完成后，仅在播放器空闲或正在播放同一首歌的流 URL 时才自动播放 |

---

### ✨ 优化

#### 广播剧生成完成后自动同步项目标题（`StudioService.java`）

| 文件 | 改动 |
|------|------|
| `StudioService.java` | `saveGeneratedContent` 增强标题解析：支持 `#标题` 和 `##标题`；drama 类型自动将解析出的标题写回 `StudioProject.title` |

**背景**：AI 大模型按 Prompt 指令在第一行输出 `#标题名`，但后端只解析 `##` 前缀且未同步到项目标题，导致用户修改设定重新生成后左上角标题不变。

---

#### AI 音乐页面 emoji 清理（`Music.vue`）

| 文件 | 改动 |
|------|------|
| `Music.vue` | 移除所有按钮、状态标签、Toast 消息中的 emoji 图标 |

---

### 📝 文档

| 文件 | 说明 |
|------|------|
| `docs/guides/sa-token-auth.md` | 新增：Sa-Token 权限鉴定架构详解（三层防御 + SSE 避坑指南） |

---

### 🚀 Git Commits

```
81e1dc4 修复: SSE fetch请求打到www域名(Cloudflare Pages)导致405
00c816d 优: 广播剧生成完成后自动同步项目标题
c60cd5a 修复: 广播剧分角色配音(TTS v2)及相关接口超时问题
61c348b docs: 增加关于 Sa-Token 权限体系及 SSE 流式接口限制的详细说明文档
```

---

## [2026-03-12] — AI 音乐歌名生成 + 前端性能优化 + AI 接口超时修复

### ✨ 新功能

#### AI 音乐自动生成歌名（`feat: AI 音乐自动生成歌名`）

| 文件 | 改动 |
|------|------|
| `MusicLyricAgent.java` | 系统 Prompt 要求 LLM 第一行输出 `TITLE: <歌名>`，一次调用同时生成歌名+歌词 |
| `MusicService.java` | 正则解析 `TITLE:` 行，返回 `{title, lyrics}`；`submitGenerate` 优先使用 AI 歌名 |
| `MusicController.java` | `GenerateRequest` 新增 `title` 字段，接收前端传入的歌名 |
| `Music.vue` | AI 写词后自动回填歌名，歌词卡底部显示「✨ AI 歌名」可编辑输入框；提交时透传 title |

**降级策略**：LLM 未按格式输出 `TITLE:` 时，自动截取 prompt 前 25 字作为标题。

---

### ⚡ 性能优化（`perf: 前端全面性能优化`）

| 文件 | 优化点 |
|------|--------|
| `Discover.vue` | 预计算波浪高度（避免模板内 Math.random）；`v-memo` 优化列表渲染；图片 `loading="lazy"`；IntersectionObserver 无限滚动替代「加载更多」按钮 |
| `App.vue` | `<keep-alive>` 缓存 Home/Discover/VoiceLibrary 页面，切换时不重新挂载 |
| `vite.config.js` | `manualChunks` 拆分 vue/vue-router/pinia/axios 为独立 chunk；`cssCodeSplit: true` |
| `request.js` | 全局默认超时从 60s 降至 10s（AI 接口单独配置） |
| `player.js` | `timeupdate` 事件 500ms 节流，减少 Pinia 无效更新 |

---

### 🐛 修复

#### AI 接口超时修复（`fix: AI接口超时修复`）

| 接口 | 超时配置 |
|------|---------|
| `music.generate` | 60s |
| `music.generateLyrics` | 60s |
| `tts.synthesizeShort` | 30s |
| `tts.submitLongText` | 30s |
| `tts.preview` | 20s |
| `tts.generatePodcast` | 120s |
| `studio.generateOutline` | 60s |

> 全局默认 10s，AI 长耗时接口单独覆盖，防止被截断。

---

## [2026-03-11] — 支付可靠性重构 + 邀请码注册 + WebSocket 修复

### 🔒 支付可靠性三层防护（`VipService.java`）

| 防护层 | 实现 |
|--------|------|
| RSA2 验签 | 调用支付宝 SDK `AlipaySignature.rsaCheckV1()` 验证签名，防伪造回调 |
| DB CAS 幂等 | `UPDATE vip_order SET status='paid' WHERE status='pending'` 原子更新，防重复处理 |
| `@Transactional` | 事务异常自动回滚，保证订单/VIP 状态最终一致 |

### 🎫 注册方式改为邀请码（`AuthService.java`）

| 改动 | 说明 |
|------|------|
| 移除短信验证码注册 | 原 `sendCode` / SMS 验证逻辑全部移除 |
| 改为邀请码制 | 注册时校验 `invite_code` 表，有效时扣减使用次数（`times_left - 1`） |
| 新增 `invite_code` 表 | `V14__invite_code.sql`，含 `code/creator_id/times_left/expired_at` |
| 昵称生成 | 使用手机号后 4 位生成默认昵称 |

### 🔧 WebSocket 修复（`PodcastWebSocketHandler.java`）

为 R2 音频上传专用线程池添加 `@Autowired` 注解，解决 Lombok 无法推断 `@Qualifier` 导致 Bean 注入失败的问题。

---


## [2026-03-09 Round 4] — 小说发布/下架修复 + 代码规范清理

### 📋 背景
小说「完结发布」后发现页不显示、发布后无下架入口、旧数据下架报错、发布 INSERT 报错、小说合成不计费等一连串关联 Bug。同时清理剩余的 HashMap 容量、if 花括号、console.log 等代码规范问题。

---

### 🐛 Bug 修复

| Commit | 问题 | 根因 | 修复 |
|--------|------|------|------|
| `d0ed21f` | 小说发布后发现页 0 条 | `publishNovel()` 只改 status，没写 work 表 | 改为复用 `publishProject()` |
| `19c2874` | 广播剧/单次生成类型发布后无下架入口 | `v-else` 只显示文字 | 补充红色「下架」按钮 |
| `f68f885` | 小说独立模板缺下架按钮 | 小说有独立 UI 模板 | 同上 |
| `09bfa57` | 旧版数据下架报错 | `unpublishProject` 找不到 Work 抛异常 | 无 Work 时直接回退项目状态 |
| `cf11752` | 发布 INSERT 报错 | `audio_url` NOT NULL + 小说无音频 | null → `""` 兜底 |
| `cf11752` | 小说合成不计费 | `StudioService` 未注入 QuotaService | 注入依赖 + onComplete 扣减配额 |

---

### 🔧 代码规范优化 (`7f943fb`)

**后端：**
- HashMap 指定初始容量：`PodcastClient`(8), `TtsV2Service`(4), `Tts1Adapter`(8/4)
- if 单行体补花括号：`Tts1Adapter`, `TtsDramaController`（阿里【强制】规则）

**前端：**
- `console.log` → `console.debug`：`player.js`(3处), `useWebSocket.js`(1处)

---

### ⚠️ 剩余（功能性 TODO，非规范问题）

| 优先级 | 位置 | 说明 |
|--------|------|------|
| 🟡 中 | `VipService.java` | 真实支付对接（mock 已确认）|
| 🟡 中 | `AuthService.java` | 真实短信对接（mock 已确认）|
| 🟢 低 | `VoiceSelector.vue` | 音色试听播放 |
| 🟢 低 | `AiInteractionService` | 边听边问 ASR→LLM→TTS 完整链路 |

---

## [2026-03-09 Round 3] — WebSocket 安全鉴权

### 📋 背景
`/ws/interaction`（边听边问）端点此前无任何身份验证。攻击者可不带 Token 直接建立 WebSocket 连接，调用语音交互接口，属于高危安全漏洞。

---

### 🔒 `InteractionWebSocketHandler.java` — 完整重写

| 类型 | 改动 |
|------|------|
| 🔴 **安全漏洞修复** | 连接建立（`afterConnectionEstablished`）时强制验证 Sa-Token |
| 🔴 **Token 提取** | 新增 `extractToken()` 方法，从 URI query params 中解析 `?satoken=xxx` |
| 🔴 **失败即断连** | Token 缺失或无效时调用 `session.close(CloseStatus.NOT_ACCEPTABLE)`，不处理任何消息 |
| 🟡 **userId 缓存** | 认证通过后将 `userId` 存入 `session.getAttributes()`，后续 handler 直接读取，避免重复校验 |
| 🟡 **守卫检查** | `handleBinaryMessage` / `handleTextMessage` 增加 `userId == null` 防御检查 |
| 🟡 **日志规范** | 全部日志带 `[InteractionWS]` 前缀和 `userId` 上下文 |
| 🟡 **JSONObject 容量** |事件 Map 统一指定初始容量（`new JSONObject(4)`）|
| 🟢 **注释完善** | 添加 Javadoc 说明协议格式、连接方式（前端 wss URL 示例）|

**前端接入方式（无需改动后端）：**
```javascript
// 连接时在 URL 中携带 token
const ws = new WebSocket(`wss://host/ws/interaction?satoken=${token}`)
```

---

### 🚀 Git Commit

```
1d7a730 fix: 补全 WebSocket 鉴权，修复边听边问接口无 Token 校验的安全漏洞
```

---

### ✅ 全部高优先项已清零

| 优先级 | 状态 | 说明 |
|--------|------|------|
| 🔴 `InteractionWebSocketHandler` 鉴权 | ✅ **已完成** | Sa-Token Token 校验 |
| 🔴 `VipService @Transactional rollbackFor` | ✅ **已完成** | 上轮 |
| 🟡 英文日志统一 | ✅ **已完成** | 多文件 |
| 🟡 HashMap 初始容量 | ✅ **已完成** | 多文件 |
| 🟡 console.log 清理 | ✅ **已完成** | 前端 |

### ⚠️ 剩余（功能性 TODO，非规范问题）

| 优先级 | 位置 | 说明 |
|--------|------|------|
| 🟡 中 | `NovelPipelineService` Stage 3 | TTS 2.0 有声书批量合成链路 |
| 🟡 中 | `VipService` | 真实支付对接（mock 已确认） |
| 🟡 中 | `AuthService` | 真实短信对接（mock 已确认） |
| 🟢 低 | `VoiceSelector.vue` | 音色试听播放 |
| 🟢 低 | `AiInteractionService` | 边听边问 ASR→LLM→TTS 完整链路 |

---



### 📋 背景
对剩余遗留问题进行扫描整改（支付和短信模块保持 mock，不在本轮范围内）。

---

### 🔧 后端变更

#### `VipService.java`

| 类型 | 改动 |
|------|------|
| 🔴 **`@Transactional` 缺 rollbackFor** | `@Transactional` → `@Transactional(rollbackFor = Exception.class)`（阿里【强制】：受检异常必须显式声明回滚）|
| 🟡 if 无花括号 | `activateVip()` 方法的早返回 `if` 语句补全花括号 |

#### `SoundReadTools.java`（5 处）

| 类型 | 改动 |
|------|------|
| 🟡 英文错误日志 | `log.error("listVoices error", e)` → `log.error("[SoundReadTools] 查询音色列表失败: userId={}", ..., e)` |
| 🟡 英文错误日志 | `log.error("generateScript error", e)` → 含 theme/emotion/wordCount 上下文 |
| 🟡 英文错误日志 | `log.error("analyzeEmotion error", e)` → 含 textLen 上下文 |
| 🟡 英文错误日志 | `log.error("synthesizeSpeech error", e)` → 含 voiceId/textLen 上下文 |
| 🟡 英文错误日志 | `log.error("listMyWorks error", e)` → 含 userId 上下文 |

#### `SpeechWebSocketClient.java`（3 处）

| 类型 | 改动 |
|------|------|
| 🟡 英文日志 | `"Received unexpected text message"` → `"[SpeechWS] 收到意外的文本消息（预期为二进制帧）"` |
| 🟡 英文日志 | `"Failed to parse message"` → `"[SpeechWS] 消息解析失败"` |
| 🟡 英文日志 | `"WebSocket error"` → `"[SpeechWS] WebSocket 连接异常"` |
| 🟢 异常消息 | `RuntimeException("Unexpected message")` → 含 type/event 期望值的中文描述 |

#### `NovelPipelineService.java`

| 类型 | 改动 |
|------|------|
| 🟡 英文警告日志 | `"Emotion annotation failed: segmentId={}, using default"` → `"[NovelPipeline] 情感标注失败，使用默认风格"` |

#### `TtsDramaService.java`

| 类型 | 改动 |
|------|------|
| 🟡 日志前缀不统一 | `"Drama引擎ERROR"` → `"[TtsDrama] WebSocket 服务端返回错误"` |

#### `Tts1Client.java`（9 处）

| 类型 | 改动 |
|------|------|
| 🟡 HashMap 无初始容量 | `synthesize()` headers: `new HashMap<>()` → `new HashMap<>(4)` |
| 🟡 HashMap 无初始容量 | `createLongTextTask()` requestBody: `new HashMap<>()` → `new HashMap<>(8)` |
| 🟡 HashMap 无初始容量 | `createLongTextTask()` headers: `new HashMap<>()` → `new HashMap<>(4)` |
| 🟡 HashMap 无初始容量 | `queryLongTextTask()` headers: `new HashMap<>()` → `new HashMap<>(4)` |
| 🟡 HashMap 无初始容量 | `buildRequestBody()` request: `new HashMap<>()` → `new HashMap<>(4)` |
| 🟡 HashMap 无初始容量 | `buildRequestBody()` app: `new HashMap<>()` → `new HashMap<>(4)` |
| 🟡 HashMap 无初始容量 | `buildRequestBody()` user: `new HashMap<>()` → `new HashMap<>(2)` |
| 🟡 HashMap 无初始容量 | `buildRequestBody()` audio: `new HashMap<>()` → `new HashMap<>(8)` |
| 🟡 HashMap 无初始容量 | `buildRequestBody()` req: `new HashMap<>()` → `new HashMap<>(4)` |
| 🟢 注释 | 各 Map 初始化处补充初始容量来源注释（字段数量说明）|

---

### 🔧 前端变更

#### `StudioWorkbench.vue`（5 处）

| 类型 | 改动 |
|------|------|
| 🟡 `console.log` | `generateMasterOutline()` 大纲调试日志 × 2：`console.log` → `console.debug`（生产环境自动屏蔽）|
| 🟡 `console.log` | 策略 2 成功提示：`console.log` → `console.debug` |
| 🟡 `console.log` | `generateChapterOutlines()` 细纲调试日志 × 2：`console.log` → `console.debug` |

---

### ⏭️ 本轮跳过（用户决策）

| 项目 | 原因 |
|------|------|
| `VipService.java` 接入真实支付 | 当前保持 mock，待 MVP 后再对接 |
| `AuthService.java` 接入真实短信 | 当前保持 mock，待 MVP 后再对接 |
| `InteractionWebSocketHandler.java` Token 鉴权 | 改动范围较大，单独 Issue 处理 |

---

### 🚀 Git Commit

```
0e83911 refactor: fix @Transactional rollbackFor, English logs, HashMap capacity, console.debug in StudioWorkbench
```

---

### ⚠️ 剩余待处理

| 优先级 | 位置 | 说明 |
|--------|------|------|
| 🔴 高 | `InteractionWebSocketHandler.java` | WebSocket 握手 Token 鉴权（安全漏洞，单独实现）|
| 🟡 中 | `NovelPipelineService.java` Stage 3 | TTS 2.0 批量合成链路接入 |
| 🟡 中 | `VipService.java` | 接入真实支付（微信/支付宝）—— 用户确认 mock |
| 🟡 中 | `AuthService.java` | 接入真实短信 —— 用户确认 mock |
| 🟢 低 | `VoiceSelector.vue` | 音色试听功能接入 |
| 🟢 低 | `player.js` / `useWebSocket.js` | 框架调试日志（带模块前缀，可接受）|

---


---

## [2026-03-09] — 代码规范整改（阿里巴巴 Java 开发手册）

### 📋 背景
引入《阿里巴巴 Java 开发手册（黄山版）》作为后端规范基准，同时对前端代码进行同步整改。
新增编码规范文档 `docs/guides/coding-standards.md`、安全检查清单 `docs/guides/security-checklist.md`、AI 辅助上下文 `CLAUDE.md`。

---

### 🔧 后端变更

#### `StudioService.java`

| 类型 | 改动 |
|------|------|
| 🔴 乱码注释 | 清理 `generateContent()`、`generateContentFromOutline()`、`rewriteSection()` 中全部乱码注释（`// ?Agent`, `// LlmRouter gent maxTokens ?` 等），改为清晰中文描述 |
| 🟡 日志无上下文 | `generateDrama()` 的 SSE 失败日志：`log.warn("SSE send failed")` → `log.warn("[StudioService] SSE 推送 token 失败: projectId={}", ...)` |
| 🟡 日志无上下文 | `rewriteSection()` 的多处 SSE 日志补充 `sectionId` 上下文 |
| 🟡 异常消息模糊 | `generateContentFromOutline()` 异常消息补充 `projectId=` 信息 |
| 🟡 日志英文 | `onError` 回调：`log.error("[StudioService] rewrite error:")` → `log.error("[StudioService] AI 段落改写异常: sectionId={}")` |
| 🟡 onError 无用户提示 | `rewriteSection()` onError 新增向前端发送 `[AI 改写失败，请重试]` |
| 🟡 if 无花括号 | `parseChapterIndex()`、`rewriteSection()` 补全大括号（阿里【强制】规则）|
| 🟢 无意义注释 | 删除 `saveGeneratedContent()` 中空 `//` 注释行 |

#### `CreativeAgentFactory.java`

| 类型 | 改动 |
|------|------|
| 🟡 冗余 `@Autowired` | 单构造函数删除 `@Autowired`（Spring 自动注入，无需显式声明）|
| 🟡 HashMap 无初始容量 | `new HashMap<>()` → `new HashMap<>(16)`（避免 8+ 种 Agent 注册时多次扩容）|
| 🟢 构造函数缺 Javadoc | 新增 `@param agents` 参数说明 |

#### `TtsDramaService.java`

| 类型 | 改动 |
|------|------|
| 🔴 **snake_case 变量名** | `context_texts` → `contextTexts`（阿里【强制】：变量名必须 camelCase）|
| 🟡 HashMap 无初始容量 | `audioParams`、`additions`、`reqParamsInner`、`taskReq` 等 6 处：`new HashMap<>()` → `new HashMap<>(4)` |
| 🟡 不规范注释 | `"拆网线"` → `"全部台词合成完毕，关闭长连接"`；`"发生对话"` → `"发起对话（保持 section_id 关联实现情感连贯）"` |
| 🟢 注释补丁 | 各 Map 初始化处补充初始容量说明注释 |

#### `BaseClient.java`

| 类型 | 改动 |
|------|------|
| 🟡 英文日志 | `log.warn("AppId is not configured")` → `log.warn("[SDK] AppId 未配置，请检查 application.yml 中的 volcengine.tts.app-id")` |
| 🟡 英文日志 | `log.warn("AccessToken is not configured")` → 同上，带配置路径提示 |

#### `SoundReadTools.java`

| 类型 | 改动 |
|------|------|
| 🟡 英文无上下文日志 | `log.info("listMyWorks called")` → `log.info("[SoundReadTools] 查询我的作品列表: userId={}", getUser().getId())` |

---

### 🔧 前端变更

#### `api/tts.js`

| 类型 | 改动 |
|------|------|
| ✅ **新增** | 新增 `dramaApi` 模块，封装 `/tts/drama/synthesize` 接口，含完整 JSDoc 注释 |

#### `views/Drama.vue`

| 类型 | 改动 |
|------|------|
| 🔴 **违规直接 axios** | 移除 `import axios from 'axios'`，改为 `import { dramaApi } from '@/api/tts'` |
| 🔴 **直接调用** | `axios.post('/api/tts/drama/synthesize', ...)` → `dramaApi.synthesize(...)` |
| 🟡 响应解包 | 适配 `request.js` 统一拦截器（已自动解包 `Result<T>.data`），直接读取 `data.audioUrl` |
| 🟡 错误消息字段 | `error.response?.data?.message` → `error.response?.data?.msg`（后端统一字段为 `msg`）|

#### `views/Vip.vue`

| 类型 | 改动 |
|------|------|
| 🟡 `console.log` | 移除 `console.log('Order created:', order)`，改为 TODO 注释说明待对接真实支付 |

#### `components/VoiceSelector.vue`

| 类型 | 改动 |
|------|------|
| 🟡 `console.log` | `previewVoice()` 中的 `console.log("播放试听:", name)` → `toastStore.show('试听功能即将开放')` |
| 🟡 静默失败 | `fetchLibrary()` 的 catch 块由 `console.error` 改为 `console.warn` + `toastStore.show('音色库加载失败，请重试')` |
| 🟢 catch 格式 | `catch(e)` → `catch (e)`（代码风格统一）|

#### `components/GlobalPlayer.vue`

| 类型 | 改动 |
|------|------|
| 🟡 `console.log` | `onOpen: () => console.log('Interactive WS connected')` → 空函数体（附注释）|
| 🟡 `console.error` | `startRecord()` 的 catch 块：`console.error('获取麦克风失败')` → `console.warn('[GlobalPlayer] 获取麦克风失败:', e)` |
| 🟢 catch 格式 | `catch(e)` → `catch (e)` |

---

### 📝 新增文档

| 文件 | 说明 |
|------|------|
| `CLAUDE.md` | Claude Code AI 辅助上下文文件（项目概览、技术栈、规范速查、踩坑记录）|
| `docs/guides/coding-standards.md` | 编码规范（基于阿里巴巴 Java 开发手册，含 Java/Vue/DB/Git 规范）|
| `docs/guides/security-checklist.md` | 安全检查清单（🔴🟡🟢 风险分级，上线必检项）|
| `docs/guides/CHANGELOG.md` | 本变更日志文件 |

---

### 🚀 Git Commits

```
8762bb2 refactor(web): remove direct axios in Drama.vue, clean console.log, add dramaApi to tts.js
a85ff1e refactor: fix naming camelCase, HashMap capacity, log messages in TtsDramaService/BaseClient/SoundReadTools
522fe5d refactor: apply Alibaba Java spec to StudioService and CreativeAgentFactory
df68bef docs: add CLAUDE.md coding-standards security-checklist based on Alibaba Java spec
```

---

### ⚠️ 待处理 TODO（非本次范围）

| 优先级 | 位置 | 说明 |
|--------|------|------|
| 🔴 高 | `InteractionWebSocketHandler.java` | WebSocket 握手 Token 鉴权（安全漏洞）|
| 🟡 中 | `NovelPipelineService.java` Stage 3 | TTS 2.0 批量合成链路接入 |
| 🟡 中 | `VipService.java` | 接入真实支付（微信/支付宝）|
| 🟡 中 | `AuthService.java` | 接入真实短信服务 |
| 🟢 低 | `VoiceSelector.vue` | 音色试听功能（播放 previewUrl）|
| 🟢 低 | `StudioWorkbench.vue` | 清理大纲调试 `console.log`（生产部署前）|

---

*Last updated: 2026-03-09*
