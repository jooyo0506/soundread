# SoundRead 变更日志

本文档记录每次代码规范整改、功能变更和重构的具体内容，按时间倒序排列。

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
