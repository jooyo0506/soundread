# SoundRead 变更日志

本文档记录每次代码规范整改、功能变更和重构的具体内容，按时间倒序排列。

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
