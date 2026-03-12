# AGENT.md — SoundRead AI 编程助手上下文文档

> 本文件为 AI 编程助手（Codex、Claude、Cursor 等）提供全面的项目上下文。
> 读完本文件后，你应能独立理解代码结构、修改任意模块、遵循既有规范。

---

## 一、项目概览

**声读 SoundRead** 是一个 AI 语音合成与内容创作平台。用户可以：
- 将文字转换为情感丰富的语音（TTS 1.0 / TTS 2.0）
- 生成 AI 双人播客、AI 短剧（多角色语音合成）
- 用 AI 创作小说、剧本、情感电台等 8 种类型内容
- 生成 AI 音乐（含歌词、歌名）
- AI 助手 Tool Calling（8 个工具函数）

**注册方式**：邀请码制（内测）。**支付**：支付宝（RSA2 验签）。

---

## 二、技术栈速查

| 层级 | 技术 | 关键说明 |
|------|------|---------|
| **后端** | Spring Boot 3.2.5 + JDK 21 | 单体架构，端口 9090 |
| **ORM** | MyBatis-Plus 3.5.6 | 雪花 ID（Long），逻辑删除 |
| **认证** | Sa-Token 1.38.0 | Token 名 `Authorization`，Session 存 userId/admin |
| **数据库** | MySQL 8.x | `sound_read` 库，22 张表 |
| **缓存** | Redis 7.x | 配额计数 + Sa-Token + 策略缓存 |
| **AI Agent** | LangChain4j 0.30.0 | OpenAI 兼容 API |
| **LLM** | 豆包/DeepSeek/Qwen/GLM/MiniMax/Gemini | 统一用 OpenAI 格式，LlmRouter 三级降级 |
| **TTS** | 火山引擎 v1.0（REST）+ v2.0（WebSocket）| SDK 在 `server/sdk/` |
| **AI 音乐** | Mureka API | |
| **对象存储** | Cloudflare R2（S3 兼容）| `r2.joyoai.xyz` |
| **MQ** | RocketMQ 2.3.0 | 少量使用 |
| **前端** | Vue 3.4 + Vite + Pinia | 15 个页面 |
| **支付** | 支付宝 | RSA2 + DB CAS 幂等 |

---

## 三、项目目录结构

```
sounds-tts/
├── server/                         ← Spring Boot 后端（主体）
│   ├── pom.xml
│   ├── sdk/                        ← 火山引擎 TTS/播客/Mureka 封装 SDK
│   └── src/main/java/com/soundread/
│       ├── SoundReadApplication.java
│       ├── adapter/                ← 外部服务适配器
│       │   ├── LlmAdapter.java         聊天模型封装
│       │   ├── R2Adapter.java          Cloudflare R2 上传/下载
│       │   ├── Tts1Adapter.java        TTS 1.0 REST
│       │   └── WhisperAdapter.java     ASR 语音识别
│       ├── agent/                  ← AI Agent 层（LangChain4j）
│       │   ├── creative/               8 种工坊类型 Agent（Template Method）
│       │   │   ├── BaseCreativeAgent.java  抽象基类（含摘要压缩记忆链）
│       │   │   ├── CreativeAgentFactory.java  工厂，typeCode→Agent 路由
│       │   │   ├── MusicLyricAgent.java   AI 音乐歌名+歌词（TITLE:格式）
│       │   │   └── [Novel/Podcast/Drama/Emotion/...]Agent.java
│       │   ├── drama/              ← 广播剧 Agent（多角色解析）
│       │   ├── emotion/            ← 情感 TTS 2.0 Agent
│       │   └── toolcalling/
│       │       ├── SmartAssistantAgent.java  AI 助手（LangChain4j AiServices）
│       │       └── SoundReadTools.java       8 个工具函数（@Tool 注解）
│       ├── common/                 ← 通用组件
│       │   ├── Result.java             统一响应 {code, message, data}
│       │   ├── ResultCode.java         业务状态码枚举
│       │   ├── RequireFeature.java     功能权限注解（AOP）
│       │   ├── FeatureCheckAspect.java 注解逻辑（调用 TierPolicyService）
│       │   └── exception/              BusinessException + 全局异常处理
│       ├── config/                 ← 配置类
│       │   ├── ai/
│       │   │   ├── LlmRouter.java      三级瀑布降级路由器（核心！）
│       │   │   ├── LlmProperties.java  yml 中 llm 节点映射
│       │   │   ├── FallbackStreamingChatModel.java  运行时降级代理
│       │   │   └── FallbackChatModel.java
│       │   ├── AsyncConfig.java    三线程池：io(10/50)/r2(5/20)/scheduled(4)
│       │   ├── SaTokenConfig.java  路由权限 + CORS
│       │   ├── RedisConfig.java    Pub/Sub 监听容器（策略缓存广播）
│       │   ├── AlipayConfig.java   支付宝 AlipayClient Bean
│       │   ├── R2Config.java       S3Client Bean
│       │   ├── WebSocketConfig.java WebSocket 端点注册
│       │   └── JacksonConfig.java  Long→String + JavaTimeModule
│       ├── controller/             ← REST 控制器
│       │   ├── AuthController.java     /api/auth/* 注册/登录/用户信息
│       │   ├── TtsController.java      /api/tts/* TTS 1.0 合成
│       │   ├── TtsV2Controller.java    /api/tts/v2/* TTS 2.0 情感合成
│       │   ├── ttsv2/                  TTS 2.0 子模块（台本/场景/增强）
│       │   ├── ttsdrama/               多角色广播剧子模块
│       │   ├── StudioController.java   /api/studio/* 创作工坊
│       │   ├── AgentController.java    /api/agent/* AI 助手对话
│       │   ├── MusicController.java    /api/music/* AI 音乐
│       │   ├── PodcastController.java  /api/podcast/* AI 播客
│       │   ├── VipController.java      /api/vip/* VIP 和支付
│       │   ├── VoiceController.java    /api/voice/* 音色库
│       │   ├── DiscoverController.java /api/discover/* 发现页
│       │   ├── CreationController.java /api/creation/* 创作记录
│       │   ├── TierPolicyController.java /api/admin/tier-policy 运营策略
│       │   └── AdminWorkController.java  /api/admin/works 运营内容审核
│       ├── job/                    ← 定时任务
│       │   ├── HeatScoreJob.java       每 5 分钟刷新 work 热度分
│       │   └── MusicService.java       （内含每 5 秒轮询音乐任务）
│       ├── mapper/                 ← MyBatis-Plus Mapper（22 张表）
│       ├── model/
│       │   ├── entity/             ← 19 个实体类（见下方数据库模型）
│       │   └── dto/                ← 7 个 DTO（请求/响应模型）
│       ├── service/                ← 业务逻辑（18 个 Service）
│       └── websocket/
│           ├── PodcastWebSocketHandler.java  /ws/podcast 播客双向流
│           └── InteractionWebSocketHandler.java /ws/interaction 边听边问
│
├── web/                            ← Vue 3 用户端
│   └── src/
│       ├── api/                    10 个 API 模块（各有独立超时配置）
│       ├── components/             GlobalPlayer/VoiceSelector/TabBar 等
│       ├── composables/            useWebSocket.js（播客 WS 封装）
│       ├── stores/                 auth/player/toast（Pinia）
│       └── views/                  15 个页面（见下方路由）
│
├── admin/                          ← Vue 3 运营后台（Works/Policy/Login）
├── docs/                           ← 所有技术文档
│   ├── sql/init/         V1-V5 初始化脚本
│   ├── sql/migration/    V6-V14 迁移脚本（V13=性能索引，V14=邀请码）
│   └── guides/           16 篇技术文档
├── README.md                       项目概览
├── CLAUDE.md                       Claude code 专用上下文
└── AGENT.md                        本文件
```

---

## 四、数据库模型（22 张表）

### 核心表

| 表名 | 实体类 | 说明 |
|------|--------|------|
| `user` | `User` | 用户主表，含 `tier_code`（等级）、`phone`、`nickname` |
| `sys_tier_policy` | `SysTierPolicy` | 等级策略：免费/VIP 配额/功能开关/LLM 模型配置 |
| `inv_code` | `InviteCode` | 邀请码表（V14），含 `times_left`、`expired_at` |
| `vip_order` | `VipOrder` | 支付订单，`status` = pending/paid/expired |
| `work` | `Work` | 已发布作品（发现页），含 `heat_score`、`review_status` |
| `user_creation` | `UserCreation` | 用户创作记录（含所有类型的元数据） |
| `studio_project` | `StudioProject` | AI 工坊项目（含 `type_code`、`inspiration`、`outline`）|
| `studio_section` | `StudioSection` | 工坊段落（含 `content`、`audio_url`、`emotion_tag`）|
| `tts_task` | `TtsTask` | TTS 1.0 长文本异步任务 |
| `music_task` | `MusicTask` | AI 音乐生成任务，`status` = pending/processing/done/failed |
| `ai_interaction` | `AiInteraction` | 边听边问交互记录 |
| `sys_voice` | `SysVoice` | 音色库（含 `supported_engines`：tts-1.0/tts-2.0）|
| `cloned_voice` | `ClonedVoice` | 用户克隆音色（声音克隆功能，开发中）|
| `ai_prompt_category` | `AiPromptCategory` | 创作提示词分类 |
| `ai_prompt_role` | `AiPromptRole` | 创作提示词角色（角色扮演型 prompt）|
| `creative_template` | `CreativeTemplate` | 创作类型模板 |

### 重要字段约定

- **ID**：雪花算法 Long，Jackson 序列化为 String（防 JS 精度丢失）
- **逻辑删除**：`deleted` tinyint，MyBatis-Plus `@TableLogic`
- **时间**：`created_at` / `updated_at`，`AutoFillHandler` 自动填充
- **tier_code**：`user`（免费）、`vip_month`、`vip_year`、`vip_forever`

---

## 五、认证与权限体系（四层防御）

```
Layer 1: Sa-Token 路由拦截（SaTokenConfig.java）
  ✅ 保护：/api/tts/**, /api/studio/**, /api/vip/**, /api/music/**, ...
  ✅ 白名单：/api/auth/**, /api/discover/works, /api/tts/voices, /api/vip/payment/alipay-notify

Layer 2: @RequireFeature 注解（FeatureCheckAspect.java）
  内部调用 TierPolicyService.hasFeature(featureCode)
  示例：@RequireFeature("ai_script") 控制 AI 台本功能

Layer 3: Service 层 IDOR 归属校验
  StudioService.getProject(id) → 自动比对 userId，无权抛异常
  NovelService.checkChapterOwnership(chapterId, userId)

Layer 4: 业务权限（音色、配额）
  VoiceService.detectVoiceEngine(voiceId) → checkUserVoicePermission()
  QuotaService.checkAndDeductXxx() → Redis 计数，超额抛异常
```

### 如何获取当前用户

```java
// 获取当前登录用户 ID
Long userId = StpUtil.getLoginIdAsLong();

// 获取完整用户对象（Service 层常用）
User user = authService.getCurrentUser();

// WebSocket 中：认证通过后存入 session
session.getAttributes().put("userId", userId);
Long userId = (Long) session.getAttributes().get("userId");
```

### WebSocket 鉴权

WebSocket 不支持自定义 Header，Token 通过 URL Query Param 传入：
```
wss://joyoai.xyz/ws/podcast?satoken=<token>
```

---

## 六、统一响应格式

所有 Controller 均返回 `Result<T>`：

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 失败
{ "code": 400, "message": "手机号格式错误", "data": null }
```

```java
// Controller 写法
return Result.ok(data);
return Result.ok();
return Result.fail("错误原因");
return Result.fail(ResultCode.UNAUTHORIZED);
```

前端 `request.js` 拦截器**自动解包** `data` 字段，Service 层直接拿到业务数据。

---

## 七、LLM 路由系统（LlmRouter.java）

**三级瀑布降级**，选择为当前用户提供服务的 LLM 模型：

```
Level 1: sys_tier_policy.resource_rules.llmProvider（运营后台热配置）
Level 2: application.yml llm.default-model（系统基线）
Level 3: Java 默认值（兜底）
```

**运行时降级**：主模型 onError → fallback-providers 链依次重试（用户无感知）

**使用方式**：

```java
// 流式模型（推荐用 WithFallback 版本）
StreamingChatLanguageModel model = llmRouter.getStreamingModelWithFallback(user);

// 同步模型
ChatLanguageModel model = llmRouter.getChatModelWithFallback(user);
```

**⚠️ 注意**：模型实例有 ConcurrentHashMap 缓存，运营刷新策略后自动调用 `invalidateCache()`。

---

## 八、TTS 系统（两套引擎）

### TTS 1.0 — 短文本/长文本（REST）

```
短文本: POST /api/tts/synthesize → Tts1Adapter.synthesize() → 返回 base64 音频
长文本: POST /api/tts/long-text  → 创建异步任务 → 轮询 /api/tts/task/{taskId}
```

### TTS 2.0 — 情感合成（WebSocket 全双工）

```
连接: wss://sjd.joyoai.xyz  (火山引擎专属 WS endpoint)
协议: 二进制帧 (Binary)
情感指令: 在文本中嵌入 #开心 #悲伤 #愤怒 等标签
```

服务端收到音频后，上传到 R2：`r2UploadExecutor` 线程池（隔离防阻塞）。

### 广播剧（多角色 TTS 2.0）

```
ScriptParser.parse(script) → 按角色分段
→ TtsDramaService.synthesize() → 每个角色独立 TTS 2.0 连接
→ 合并返回段落列表（含各角色 audio_url）
```

---

## 九、AI 创作工坊（8 种类型）

**架构**：Template Method 模式 + LangChain4j

```java
// 工厂路由
BaseCreativeAgent agent = creativeAgentFactory.getAgent(typeCode);

// Template Method
agent.generateContent(project, section, sseEmitter, user);
  ├── buildSystemPrompt()     // 各子类实现（定制 Prompt）
  ├── buildUserPrompt()       // 各子类实现
  └── streamGenerate()        // 基类统一：调 LlmRouter → 流式推送 SSE
```

**typeCode 列表**：`novel` / `drama` / `podcast` / `emotion_radio` /
`knowledge` / `ecommerce` / `audio_picture_book` / `news_broadcast`

**摘要压缩记忆链**（`SectionMemoryService.java`）：
- 每章生成完成后，用 LLM 压缩为 200 字摘要
- 下一章 Prompt 附带历史摘要，保持故事连贯
- 存储于 pgvector（向量数据库，可选）

---

## 十、配额系统（QuotaService.java）

配额存于 `sys_tier_policy.quota_limits`（JSON 字段），通过 Redis 计数。

| 配额字段 | 含义 |
|---------|------|
| `tts_daily_chars` | TTS 1.0 每日字数 |
| `tts_v2_daily_chars` | TTS 2.0 每日字数 |
| `ai_script_daily_count` | AI 工坊每日次数 |
| `podcast_daily_count` | AI 播客每日次数 |
| `music_daily_count` | AI 音乐每日次数 |
| `novel_daily_chars` | AI 小说每日字数 |
| `max_projects` | 工坊项目数上限 |

```java
// 扣减示例
quotaService.checkAndDeductTts(userId, charCount);  // 超额自动抛 BusinessException
```

---

## 十一、策略缓存一致性

运营后台修改 `sys_tier_policy` 后，通过 **Redis Pub/Sub** 广播刷新：

```
TierPolicyController.update() → refreshAll()
  → 写 Redis (sys:policy:*)
  → publish("soundread:policy:refresh", timestamp)
       ↓ 所有订阅节点
  生产服务器收到 → localCache.clear()
  → 下次读取时 L1 miss → 从 Redis 读取最新
```

---

## 十二、支付系统（VipService.java）

**支付宝扫码支付流程**：

```
1. POST /api/vip/payment/alipay  → 生成二维码 URL，创建 vip_order(status=pending)
2. 用户扫码支付
3. 支付宝 POST /api/vip/payment/alipay-notify
   └── AlipaySignature.rsaCheckV1() 验签（防伪造）
   └── CAS: UPDATE SET status='paid' WHERE status='pending'（防重复）
   └── @Transactional activateUserVip()（防脏数据）
   └── return "success"
4. 前端轮询 GET /api/vip/order/{orderNo}/status
```

---

## 十三、前端路由 & 页面

```
/            → Home.vue          首页（AI 工具入口）
/discover    → Discover.vue      发现页（IntersectionObserver 无限滚动）
/studio      → Studio.vue        工坊项目列表
/studio/workbench/:id → StudioWorkbench.vue  工坊编辑器（最复杂，214KB）
/create      → Create.vue        TTS 1.0 快速合成
/emotion     → Emotion.vue       TTS 2.0 情感合成
/podcast     → Podcast.vue       AI 播客
/music       → Music.vue         AI 音乐
/ai-workshop → AiWorkshop.vue    AI 助手 Tool Calling
/profile     → Profile.vue       个人中心
/vip         → Vip.vue           VIP 购买页
/login       → Login.vue         登录/注册（邀请码制）
/my-creations → MyCreations.vue  我的创作记录
/voice-library → VoiceLibrary.vue  音色库
/pay-result  → PayResult.vue     支付结果页
```

**前端 keep-alive**：Home/Discover/VoiceLibrary 三个页面被缓存，切换不重新挂载。

**API 超时配置**（`src/api/request.js`）：
- 全局默认：10s
- AI 接口单独：`generatePodcast` 120s，`generate`/`generateLyrics` 60s，`synthesizeShort` 30s

---

## 十四、开发规范（必读！）

### 后端规范（阿里巴巴 Java 开发手册）

1. **返回值**：永远返回 `Result<T>`，不直接返回实体
2. **HashMap**：必须指定初始容量，如 `new HashMap<>(8)`
3. **@Transactional**：必须加 `rollbackFor = Exception.class`
4. **if 语句**：单行体必须加花括号（阿里强制）
5. **日志格式**：`log.xxx("[模块名] 操作描述: key={}", value)`，中文描述，带上下文
6. **Long ID**：序列化为 String（JacksonConfig 全局配置），前端不需要处理
7. **逻辑删除**：使用 MyBatis-Plus `@TableLogic`，用 `deleted = 1` 而非物理删除

### 前端规范

1. **API 调用**：通过 `src/api/*.js` 模块，不直接用 axios
2. **console.log**：改为 `console.debug`（生产环境自动屏蔽）
3. **错误提示**：通过 `toastStore.show(msg)` 展示，不用 alert
4. **响应解包**：拦截器已自动解包 `Result.data`，Service 直接读业务数据

### 代码新增流程

新增一个功能模块（以 AI 功能为例）：
1. `model/entity/Xxx.java` → 实体 + MyBatis-Plus 注解
2. `mapper/XxxMapper.java` → 继承 BaseMapper<Xxx>
3. `service/XxxService.java` → 业务逻辑（注入 QuotaService 扣减配额）
4. `controller/XxxController.java` → REST 端点，加 `@RequireFeature` 或手动权限校验
5. `SaTokenConfig.java` → 确认路由已在保护范围内
6. `docs/sql/migration/V{N}__xxx.sql` → 数据库迁移
7. `web/src/api/xxx.js` → 前端 API 模块（含超时配置）

---

## 十五、常见坑 & 注意事项

| 陷阱 | 说明 |
|------|------|
| **`@Qualifier` + `@RequiredArgsConstructor`** | Lombok 不传递字段注解到构造函数，需改用 `@Autowired` + `@Qualifier` 字段注入 |
| **BOM 字符** | Windows 上用 PowerShell 写 Java 文件会加 UTF-8 BOM，导致编译报 `\ufeff` 非法字符 |
| **WebSocket 双 /ws 路径** | 前端 `VITE_WS_BASE_URL=wss://xxx/ws` + endpoint `/podcast`，不要在 endpoint 里加 `/ws` 前缀 |
| **MiniMax Tool Calling** | MiniMax 非 OpenAI 标准格式，不能加入 `TOOL_CALLING_PROVIDERS` |
| **SSE + Sa-Token** | SSE 请求务必带 `Authorization` Header（`localStorage.getItem('sr_token')`） |
| **支付回调白名单** | `/api/vip/payment/alipay-notify` 必须在 Sa-Token 白名单，否则无 Token 被拦截 |
| **LlmRouter 缓存** | 运营刷新策略后自动调用 `llmRouter.invalidateCache()`，模型实例会重建 |
| **Redis 配额 Key** | 格式：`quota:{userId}:{fieldName}:{date}`，每日 0 点自动过期 |
| **声音克隆/边听边问** | 功能代码已有框架，**但未正式上线**，前端入口隐藏或返回占位响应 |

---

## 十六、本地开发快速启动

```bash
# 1. 后端（需本地 MySQL 8 + Redis）
cd server
# 复制配置并填入密钥
cp src/main/resources/application-example.yml src/main/resources/application.yml
mvn clean package -DskipTests
java -jar target/soundread-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev

# 2. 前端
cd web && pnpm install && pnpm dev
# 访问 http://localhost:5173

# 3. 运营后台（可选）
cd admin && pnpm install && pnpm dev
```

**必需的环境变量**（`application.yml` 中通过 `${KEY:default}` 读取）：

| 变量 | 用途 |
|------|------|
| `DB_PASSWORD` | MySQL 密码 |
| `REDIS_PASSWORD` | Redis 密码 |
| `REDIS_HOST` | Redis 主机 |
| `VOLCENGINE_APP_ID` | 火山引擎 TTS AppId |
| `VOLCENGINE_ACCESS_TOKEN` | 火山引擎 TTS Token |
| `ALIPAY_APP_ID` | 支付宝 AppId |
| `ALIPAY_PRIVATE_KEY` | 支付宝 RSA 私钥 |
| `ALIPAY_PUBLIC_KEY` | 支付宝公钥 |
| `R2_ACCESS_KEY` | Cloudflare R2 访问密钥 |
| `R2_SECRET_KEY` | Cloudflare R2 密钥 |
| `llm.api-keys.doubao` | 豆包 API Key |

---

## 十七、生产部署

- **前端**：Cloudflare Pages（自动部署，`www.joyoai.xyz`）
- **API**：`joyoai.xyz/api` → Nginx → Spring Boot 9090（新加坡 VPS，免 ICP 备案）
- **CI/CD**：Jenkins Pipeline（`docs/guides/deployment.md`）
- **日志**：`/opt/sounds-tts/app.log`，PID 文件 `/opt/sounds-tts/app.pid`

---

*Last updated: 2026-03-12*
