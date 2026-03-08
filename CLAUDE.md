# SoundRead — Claude Code 项目上下文

> 本文件供 Claude Code 在每次会话开始时自动读取，请保持内容精准、简洁。

---

## 项目概览

**声读 SoundRead** 是一个 AI 语音合成与内容创作平台，面向 C 端用户提供：

- 🎙️ **多引擎 TTS 合成**（火山引擎 TTS 1.0 HTTP + TTS 2.0 Seed 双向流式）
- 🎭 **情感配音 / AI 剧本**（角色扮演、情感标注、实时 SSE 流式输出）
- 🎧 **AI 播客生成**（双声道对话，WebSocket 全双工协议）
- 📚 **AI 有声书**（小说自动切章 → 情感标注 → TTS 批量合成）
- 🎨 **AI 创作工坊 Studio**（Novel/Drama/Podcast/Radio/Lecture/Ad 六大内容类型）
- 🎵 **AI 音乐生成**（Mureka 接入）

---

## 目录结构

```
soundread/
├── server/          # Spring Boot 后端（Java 17）
│   ├── src/main/java/com/soundread/
│   │   ├── adapter/         # 第三方 API 适配器（Tts1Adapter 等）
│   │   ├── agent/           # LangChain4j AI Agent（creative/drama/emotion/toolcalling）
│   │   ├── common/          # 公共类（Result、BusinessException、R 等）
│   │   ├── config/          # Spring 配置（Sa-Token、MybatisPlus、WebSocket 等）
│   │   ├── controller/      # REST 接口层（14 个 Controller）
│   │   ├── job/             # 定时任务（DataRetentionJob）
│   │   ├── mapper/          # MyBatis-Plus Mapper 接口
│   │   ├── model/entity/    # 数据库实体
│   │   ├── sdk/             # 自封装 SDK（TTS v1/v2、Podcast、Mureka）
│   │   ├── service/         # 业务服务层
│   │   └── websocket/       # WebSocket Handler（Podcast、Interaction）
│   └── src/main/resources/
│       ├── application-example.yml  # 配置模板（不含真实密钥）
│       └── logback-spring.xml
├── web/             # Vue 3 前端（Vite + Pinia + Vue Router）
│   └── src/
│       ├── views/           # 页面组件（Create/Emotion/Drama/Podcast/Novel/Studio 等）
│       ├── components/      # 公共组件（NavBar/TabBar/GlobalPlayer/VoiceSelector）
│       ├── api/             # Axios 接口封装
│       ├── stores/          # Pinia 状态管理（auth/player/toast）
│       └── router/          # 路由配置
├── docs/            # 技术文档、变更日志、API 参考
├── design/          # UI 设计稿
└── admin/           # 管理后台（独立项目）
```

---

## 技术栈

### 后端（server/）

| 组件 | 版本 / 说明 |
|------|-----------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| 认证 | Sa-Token 1.38.0（JWT 模式，Redis 存储）|
| ORM | MyBatis-Plus 3.5.6（逻辑删除、雪花 ID）|
| 数据库 | MySQL 8（业务数据）+ PostgreSQL（pgvector 向量存储）|
| 缓存 | Redis（Session、配额、Agent Memory）|
| AI 框架 | LangChain4j 0.30.0（AiServices、Streaming、RAG）|
| LLM | 豆包（默认）/ DeepSeek / Qwen / MiniMax（三级瀑布降级）|
| 本地 Embedding | all-MiniLM-L6-v2 ONNX（无 API 费用）|
| 向量存储 | pgvector（章节 RAG 上下文）|
| 存储 | Cloudflare R2（S3 兼容，音频/封面）|
| HTTP 客户端 | OkHttp 4.12.0 |
| JSON | Fastjson2 2.0.47 |
| 消息队列 | RocketMQ 2.3.0 |
| 构建 | Maven |

### 前端（web/）

| 组件 | 版本 / 说明 |
|------|-----------|
| 框架 | Vue 3.4 + Composition API |
| 构建工具 | Vite 5.4 |
| 路由 | Vue Router 4 |
| 状态管理 | Pinia 2.3 |
| HTTP | Axios 1.13 |
| CSS | Tailwind CSS 3.4 |

---

## 常用命令

### 后端

```bash
# 编译检查
cd server && mvn compile

# 启动开发服务器（需先配置 application.yml）
cd server && mvn spring-boot:run

# 打包
cd server && mvn package -DskipTests

# 清理
cd server && mvn clean
```

### 前端

```bash
# 安装依赖
cd web && npm install

# 开发模式（端口 5173）
cd web && npm run dev

# 生产构建
cd web && npm run build

# 预览构建产物
cd web && npm run preview
```

---

## 核心业务模块

### 1. TTS 引擎路由（关键）

- **TTS 1.0**（`Tts1Adapter`）：HTTP 同步接口，适合 ≤500 字短文
- **TTS 2.0 Seed**（`TtsV2Controller` / `sdk/tts/v2/`）：WebSocket 双向流式，支持情感标注、实时字幕、`section_id` 跨段记忆
- **Podcast TTS**（`PodcastWebSocketHandler`）：双人对话专用，二进制帧协议
- **引擎选择**：`VoiceService.detectVoiceEngine()` 根据音色 ID 和配置自动路由

音频时长估算标准：**4.5 字/秒**（TTS 平均语速），见 `StudioService.publishProject()`

### 2. LLM 路由与降级（LlmRouter）

三级瀑布降级策略：

```
Level 1: sys_tier_policy.resource_rules（运营后台配置，每用户等级）
Level 2: application.yml ai.llm.*（系统基线）
Level 3: Java 代码硬编码默认值
```

调用入口：`llmRouter.getChatModelWithFallback(user)` / `getStreamingModelWithFallback(user, maxTokens)`

### 3. AI 创作工坊 Studio（StudioService）

状态流转：`draft` → `creating` → `editing` → `completed`

- Agent 工厂：`CreativeAgentFactory.getAgent(typeCode)` 返回对应 Agent
- 内容生成：SSE 流式推送，`SseEmitter`
- 段落记忆：`SectionMemoryService`（摘要记忆 + pgvector RAG 检索）
- 发布：`StudioProject` → `UserCreation` → `Work`（发现页）

### 4. AI 有声书 Pipeline（NovelPipelineService）

```
Stage 1: ChapterSplitterAgent → novel_chapter（章节切割）
Stage 2: EmotionAnnotatorAgent → novel_segment（情感标注 + CoT）
Stage 3: TODO: TTS 2.0 批量合成（section_id 关联）
```

### 5. 存储配额（StorageQuotaService）

- 每次上传/删除同步更新 `user_storage.used_bytes`
- 配额来源：`user_storage.quota_override_mb`（个人覆盖）> `sys_tier_policy`（等级策略）
- -1 表示无限存储

---

## 数据库实体关键说明

| 实体 | 表名 | 说明 |
|------|------|------|
| `User` | `user` | 用户，含 `tier_code` 等级字段 |
| `UserCreation` | `user_creation` | 每次 TTS 创作记录（含音频 URL、文件大小）|
| `UserStorage` | `user_storage` | 存储配额统计 |
| `Work` | `work` | 发现页公开作品 |
| `SysVoice` | `sys_voice` | 系统音色库 |
| `StudioProject` | `studio_project` | 创作工坊项目 |
| `StudioSection` | `studio_section` | 创作工坊项目的段落 |
| `NovelProject` | `novel_project` | 有声书项目 |
| `NovelChapter` | `novel_chapter` | 有声书章节 |
| `NovelSegment` | `novel_segment` | 有声书语义段落（TTS 最小单元）|
| `SysTierPolicy` | `sys_tier_policy` | 用户等级策略（JSON 规则）|

**ID 策略**：所有主键使用雪花算法（`IdType.ASSIGN_ID`），序列化时通过 `@JsonSerialize(using = ToStringSerializer.class)` 转为字符串，防止 JS 精度损失。

**逻辑删除**：`@TableLogic` 注解，`deleted` 字段（0=正常，1=已删除），MyBatis-Plus 自动过滤。

---

## API 接口规范

### 统一响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

封装类：`com.soundread.common.Result<T>`

### 认证方式

- Header：`Authorization: Bearer <token>`
- Sa-Token 验证，拦截器配置在 `config/SaTokenConfig.java`
- 忽略路径：`/api/auth/**`、`/api/discover/**`（公开接口）

### 主要路由前缀

| 前缀 | Controller | 说明 |
|------|-----------|------|
| `/api/auth` | `AuthController` | 登录、注册、验证码 |
| `/api/tts` | `TtsController` | TTS 1.0 合成 |
| `/api/tts/v2` | `TtsV2Controller` | TTS 2.0 Seed 流式 |
| `/api/tts/drama` | `TtsDramaController` | 剧本配音 |
| `/api/voice` | `VoiceController` | 音色管理 |
| `/api/creation` | `CreationController` | 创作记录管理 |
| `/api/podcast` | `PodcastController` | AI 播客 |
| `/api/novel` | `NovelController` | 有声书 |
| `/api/studio` | `StudioController` | 创作工坊 |
| `/api/discover` | `DiscoverController` | 发现页（公开）|
| `/api/agent` | `AgentController` | AI 对话 / Tool Calling |
| `/api/vip` | `VipController` | 会员 |
| `/api/admin` | `AdminWorkController` | 运营管理 |

---

## 配置文件

**本地配置（不进 Git）**：`server/src/main/resources/application.yml`

**模板**：`server/src/main/resources/application-example.yml`

关键环境变量：

```bash
DB_PASSWORD          # MySQL 密码
REDIS_PASSWORD       # Redis 密码
JWT_SECRET           # JWT 签名密钥
DOUBAO_API_KEY       # 豆包 LLM API Key
DOUBAO_MODEL_ID      # 豆包模型 Endpoint ID
VOLCENGINE_APP_ID    # 火山引擎 TTS App ID
VOLCENGINE_ACCESS_TOKEN  # 火山引擎 TTS Token
R2_ACCESS_KEY_ID     # Cloudflare R2 Access Key
R2_SECRET_ACCESS_KEY # Cloudflare R2 Secret Key
R2_BUCKET_NAME       # R2 存储桶名称
R2_ENDPOINT          # R2 Endpoint URL
R2_PUBLIC_DOMAIN     # R2 公网 CDN 域名
```

---

## 编码规范

### Java

- **注释语言**：中文（所有 Javadoc、内联注释均使用中文）
- **日志格式**：`log.info("[模块名] 操作描述: key1={} key2={}", val1, val2)`
- **异常**：业务异常使用 `BusinessException`，运行时异常使用中文消息
- **Controller**：仅做参数校验和权限验证，业务逻辑下沉到 Service
- **Service 方法命名**：动词 + 名词，如 `generateContent()`、`publishProject()`
- **不用 `@Autowired`**：全部使用构造函数注入（Lombok `@RequiredArgsConstructor`）
- **枚举值**：状态字段统一用字符串（如 `"draft"/"creating"/"editing"/"completed"`）

### Vue

- **Composition API**：`<script setup>` 语法
- **API 调用**：统一从 `src/api/` 目录引入，不直接在组件内写 axios
- **状态管理**：全局状态用 Pinia store，组件内局部状态用 ref/reactive
- **样式**：Tailwind CSS utility classes，避免内联 style

---

## AI Agent 架构

```
AgentController（HTTP/SSE入口）
    └── CreativeAgentFactory.getAgent(typeCode)
            ├── NovelAgent       → 有声书内容生成
            ├── DramaAgent       → 剧本对话生成
            ├── PodcastAgent     → 播客脚本生成
            ├── RadioAgent       → 情感广播生成
            ├── LectureAgent     → 知识讲解生成
            └── AdAgent          → 广告文案生成

每个 Agent 实现接口：
    buildInspirationPrompt()      → 灵感种子
    buildOutlinePrompt()          → 基础大纲
    buildStructuredOutlinePrompt()→ 结构化大纲
    buildContentSystemPrompt()    → 正文生成
    buildRewriteSystemPrompt()    → 段落改写
    buildChapterUserMessage()     → 章节正文（小说专用）
```

**记忆方案**（`SectionMemoryService`）：

1. 摘要记忆：前 N 段内容摘要拼接进 System Prompt
2. RAG 检索：pgvector + all-MiniLM-L6-v2 本地 Embedding，检索最相关历史段落

---

## 常见开发场景

### 新增一个 TTS 合成接口

1. 在 `controller/` 添加 `@PostMapping`
2. 在 `service/` 编写业务逻辑（调用 `Tts1Adapter` 或 `TtsV2` SDK）
3. 合成成功后调用 `storageQuotaService.addStorage()` 更新配额
4. 保存 `UserCreation` 记录

### 新增一个 Agent 内容类型

1. 在 `agent/creative/` 创建 `XxxAgent.java` 实现 `CreativeAgent` 接口
2. 在 `CreativeAgentFactory` 的 switch 中注册 typeCode
3. 在 `sys_creative_template` 表中添加对应模板记录
4. 前端 `StudioWorkbench.vue` 中添加对应 UI 卡片

### 修改 LLM 模型配置

- 运营层（最高优先级）：`sys_tier_policy.resource_rules` JSON 字段
- 系统层：`application.yml` 的 `ai.llm.*` 配置
- 代码层（兜底）：`LlmRouter.java` 中的默认值

---

## 注意事项（踩坑记录）

1. **application.yml 不进 Git**：含数据库密码等敏感信息，已在 `.gitignore` 中排除，使用 `application-example.yml` 作为模板
2. **Long 型 ID 序列化**：前端 JS Number 精度问题，所有 Long 类型 ID 字段必须加 `@JsonSerialize(using = ToStringSerializer.class)`
3. **逻辑删除**：查询时 MyBatis-Plus 自动追加 `WHERE deleted = 0`，手动 SQL 需注意
4. **TTS 音频时长估算**：统一使用 **4.5 字/秒** 标准，不要使用其他数值
5. **SSE 错误推送**：AI 生成异常时向前端发送 `"[AI 生成失败，请重试]"`，不要使用无意义占位字符串
6. **LF/CRLF**：Windows 开发环境，git 会提示 LF→CRLF 警告，属正常现象，无需处理
7. **pgvector**：向量存储仅用于 Studio 段落 RAG，需要 PostgreSQL 安装 `vector` 扩展

---

## Git 工作流

```bash
# 当前唯一分支
main   →   https://github.com/jooyo0506/soundread

# 提交规范（Conventional Commits）
feat:   新功能
fix:    Bug 修复
refactor: 重构
docs:   文档更新
style:  代码格式
chore:  构建/配置变更
```

---

## 规范文档（必读）

> 以下文档是本项目编码和安全的完整规范，Claude Code 在生成代码前应先参考。

| 文档 | 路径 | 说明 |
|------|------|------|
| 编码规范 | `docs/guides/coding-standards.md` | 基于阿里巴巴 Java 开发手册，含 Java/Vue/DB/Git 规范 |
| 安全检查清单 | `docs/guides/security-checklist.md` | 接口鉴权、SQL 安全、密钥管理、部署安全 |

**核心规范速查（Claude Code 生成代码时必须遵守）：**

1. **注释**：全部使用中文，方法级必须写 Javadoc
2. **日志**：格式 `[模块名] 描述: key={}` ，禁止 `log.info("info", ...)`
3. **异常**：必须有具体上下文，禁止 `"operation failed"` / `"error"`
4. **注入**：`@RequiredArgsConstructor` 构造函数注入，禁止 `@Autowired` 字段注入
5. **ID 序列化**：Long 类型 ID 必须加 `@JsonSerialize(using = ToStringSerializer.class)`
6. **魔法值**：禁止硬编码数字/字符串，必须定义常量
7. **事务**：写操作加 `@Transactional(rollbackFor = Exception.class)`
8. **布尔字段**：禁止 `isXxx` 命名（Lombok 序列化问题），使用 `enable` / `published`
9. **POJO 字段**：不在字段上设默认值（如 `= 0` / `= ""`）
10. **等号比较**：常量放左边，如 `"draft".equals(status)`，防止 NPE

---

*Last updated: 2026-03-09*
