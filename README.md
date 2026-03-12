# 🎙️ 声读 SoundRead

> **让文字发声，为内容赋能** — 新一代 AI 语音合成与内容创作平台

---

## 📋 产品定位

一款面向内容创作者的一站式 AI 语音平台，致力于将普通文本转化为具备情绪、多角色交互的专业级语音内容。

### 目标用户

| 用户群体 | 核心场景 |
|---------|---------|
| 🎬 自媒体/短视频创作者 | 高效批量配音、多音轨导出、字幕生成 |
| 📖 小说/有声书作者 | 长文本稳定合成、多角色音色绑定、情感标注 |
| 🏢 企业/教育用户 | 宣传片配音、课程音频、跨语言合成 |
| 🎧 普通听众 | 通勤听文章、AI 双人播客、边听边问 AI 语音交互 |

### 商业模式

| 层级 | 价格 | 核心权益 |
|------|------|---------|
| 免费用户 | ¥0 | 每日 100 字基础 TTS、3 次 AI 互动 |
| VIP 月度 | ¥30/月 | 无限字数/天、情感合成、AI 播客、创作工坊、音乐生成 |
| VIP 年度 | ¥300/年 | 无限字数、50 个创作项目、200+ 创作历史 |
| VIP 终身 | ¥1000 | 全部无限 |

> 注册方式：**邀请码制**（内测阶段，邀请码一次有效，支持支付宝扫码开通 VIP）

---

## ✨ 功能矩阵

| 模块 | 功能 | 技术亮点 | 状态 |
|------|------|---------|------|
| 🗣️ TTS 1.0 | 短文本/长文本语音合成 | 火山引擎 REST + 异步任务 | ✅ 已上线 |
| 🎭 TTS 2.0 | 情感合成 + AI 台本 | WebSocket 全双工 + 情感指令 (`#开心` `#悲伤`) | ✅ 已上线 |
| 🎬 AI 短剧 | 多角色对话 + TTS 合成 | 角色解析 + 分角色音色绑定 | ✅ 已上线 |
| 🎙️ AI 播客 | 双人对话式播客 | LLM 多轮生成 + 双人播客 TTS | ✅ 已上线 |
| 🎵 AI 音乐 | 歌曲/纯音乐/歌词+**歌名**生成 | Mureka API + HLS 流式播放 + 歌词同步 + AI 自动生成匹配歌名 | ✅ 已上线 |
| 📝 AI 创作工坊 | 8 种创作类型 | Template Method 模式 + 摘要压缩记忆链 | ✅ 已上线 |
| 📖 有声书 | 长篇小说 → 有声书 | 4 阶段异步 Pipeline (分章→分段→标注→合成) | ✅ 已上线 |
| 🗣️ 声音克隆 | 上传样本 → 个人音色 | Whisper 质检 + 火山引擎克隆 API | 🚧 开发中，未上线 |
| 💬 边听边问 | 收听时语音提问 | ASR → LLM → TTS 全链路语音闭环 | 🚧 开发中，未上线 |
| 🤖 AI 助手 | Tool Calling 智能助手 | LangChain4j Agent + 8 个工具函数 | ✅ 已上线 |

### AI 创作工坊 — 8 种类型

| 类型 | 图标 | 说明 |
|------|:----:|------|
| AI 小说创作 | 📖 | 网络小说章节创作 |
| AI 短剧 | 🎬 | 爆款短剧对话剧本 |
| AI 播客 | 🎙️ | 双人对话式播客 |
| 情感电台 | 🌙 | 深夜治愈系独白 |
| 知识讲解 | 📚 | 通俗化专业知识 |
| 带货文案 | 🛒 | 极具感染力的产品推荐 |
| 有声绘本 | 🎨 | 儿童有声故事 |
| 新闻播报 | 📰 | 专业新闻播报稿 |

---

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Layer                              │
│   ┌─────────────┐    ┌──────────────┐    ┌───────────────┐  │
│   │  web/ (Vue3) │    │ admin/ (Vue3) │    │ design/ (HTML)│  │
│   │  15 pages    │    │  3 pages      │    │  8 mockups    │  │
│   └──────┬──────┘    └──────┬───────┘    └───────────────┘  │
│          │ REST/SSE/WS      │ REST                           │
├──────────┼──────────────────┼────────────────────────────────┤
│          ▼                  ▼        server/ (Spring Boot 3) │
│   ┌──────────────────────────────────────────────────────┐   │
│   │  Controller Layer (16 + 2 子模块, 10 子 Controller)   │   │
│   └──────────────────────┬───────────────────────────────┘   │
│                          ▼                                    │
│   ┌──────────────────────────────────────────────────────┐   │
│   │  AI Agent Layer (LangChain4j)                         │   │
│   │  creative/ (10) + emotion/ (4) + drama/ (2)           │   │
│   │  + toolcalling/ (SmartAssistant + Tools)              │   │
│   └──────────────────────┬───────────────────────────────┘   │
│                          ▼                                    │
│   ┌──────────────────────────────────────────────────────┐   │
│   │  Service Layer (21 Services + 2 Impl)                 │   │
│   └──────────────────────┬───────────────────────────────┘   │
│                          ▼                                    │
│   ┌──────────────────────────────────────────────────────┐   │
│   │  Adapter Layer (4)      │  SDK Layer                  │   │
│   │  LLM / R2 / TTS / ASR  │  tts/ podcast/ mureka/      │   │
│   └──────────────────────┬───────────────────────────────┘   │
│                          ▼                                    │
│   ┌──────────────────────────────────────────────────────┐   │
│   │  Infrastructure                                       │   │
│   │  MySQL 8 │ Redis │ R2 │ pgvector │ 火山TTS │ 多供应商LLM│   │
│   └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈

| 类别 | 技术 | 版本/说明 |
|------|------|---------|
| **核心框架** | Spring Boot | 3.2.5 |
| **前端框架** | Vue 3 + Vite + Pinia | Vue 3.4，keep-alive 缓存优化 |
| **认证鉴权** | Sa-Token + JWT | 1.38.0 |
| **ORM** | MyBatis-Plus | 3.5.6 |
| **数据库** | MySQL | 8.x |
| **缓存** | Redis | 7.x |
| **向量数据库** | PostgreSQL + pgvector | 15 + 0.8.x（RAG 记忆引擎）|
| **对象存储** | Cloudflare R2 (S3 兼容) | — |
| **消息队列** | RocketMQ | 2.3.0 |
| **AI Agent** | LangChain4j | 0.30.0 |
| **LLM** | 豆包/DeepSeek/Qwen/GLM/MiniMax/Gemini | 三级瀑布降级路由 |
| **TTS** | 火山引擎 v1.0 + v2.0 | WebSocket 双向流 |
| **AI 音乐** | Mureka API | — |
| **ASR** | Whisper | v1 |
| **支付** | 支付宝 | RSA2 验签 + CAS 幂等防重 |

### LLM 路由降级链

```
用户绑定模型（Level 1）
    ↓ 失败
系统基线模型 Qwen/GLM/Gemini（Level 2）
    ↓ 失败
最终兜底（Level 3）
```

三级瀑布降级，主模型失败后用户无感知自动切换。

---

## 🚀 快速启动

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| pnpm | 8+ |
| MySQL | 8.x |
| Redis | 6+ |
| PostgreSQL + pgvector | 15（可选，RAG 功能需要）|

### 启动步骤

```bash
# 1. 克隆项目
git clone <repo-url>
cd soundread

# 2. 初始化数据库（按顺序执行 V1 ~ V14）
mysql -u root -p < docs/sql/init/V1__core_schema.sql
mysql -u root -p < docs/sql/init/V2__novel_schema.sql
mysql -u root -p < docs/sql/init/V3__studio_schema.sql
mysql -u root -p < docs/sql/init/V4__prompt_library.sql
mysql -u root -p < docs/sql/init/V5__music_schema.sql
# 执行迁移脚本: docs/sql/migration/V6__* 到 V14__invite_code.sql

# 3. 配置环境变量
cp server/src/main/resources/application-example.yml server/src/main/resources/application.yml
# 编辑 application.yml 填入真实密钥

# 4. 启动后端
cd server && mvn clean package -DskipTests
java -jar target/soundread-server-1.0.0-SNAPSHOT.jar

# 5. 启动前端
cd web && pnpm install && pnpm dev

# 6. 启动运营后台 (可选)
cd admin && pnpm install && pnpm dev
```

---

## 📁 项目结构

```
soundread/
├── server/              → Spring Boot 后端服务
│   ├── pom.xml
│   ├── sdk/             → 火山引擎 TTS / 播客 / Mureka SDK
│   └── src/main/java/com/soundread/
│       ├── adapter/     → 外部服务适配 (LLM/R2/TTS/ASR)
│       ├── agent/       → AI Agent (creative/emotion/drama/toolcalling)
│       │   └── creative/   → MusicLyricAgent（同时生成歌名+歌词）
│       ├── common/      → 统一响应体 + AOP + 异常处理
│       ├── config/      → 配置 (Sa-Token/R2/WebSocket/AI路由/Redis)
│       ├── controller/  → REST 控制器 (16 + 2 子模块)
│       ├── entity/      → 扩展实体 (Novel/Voice/Creation/Music)
│       ├── job/         → 定时任务 (热度计算/数据清理)
│       ├── mapper/      → MyBatis-Plus Mapper (22 张表含 invite_code)
│       ├── model/       → 数据模型 (13 Entity + 7 DTO)
│       ├── sdk/         → 底层 SDK 封装
│       ├── service/     → 业务逻辑 (21 Service)
│       └── websocket/   → WebSocket (播客/边听边问)
│
├── web/                 → Vue 3 用户端
│   └── src/
│       ├── api/         → 10 个 API 模块（AI 接口独立超时配置）
│       ├── components/  → 5 个全局组件
│       ├── composables/ → WebSocket 组合式函数
│       ├── stores/      → 3 个 Pinia Store（player 已节流优化）
│       └── views/       → 15 个页面（Discover 无限滚动优化）
│
├── admin/               → Vue 3 运营后台
│   └── src/
│       ├── api/         → 3 个 API 模块
│       ├── components/  → AdminLayout
│       ├── stores/      → 2 个 Pinia Store
│       └── views/       → 3 个页面 (Works/Policy/Login)
│
├── docs/                → 技术文档（16 篇指南）
│   ├── sql/             → 统一 SQL 脚本 (init V1-V5 / migration V6-V14)
│   ├── guides/          → 技术指南（部署/性能/安全/支付/配额等）
│   ├── ai-music/        → AI 音乐模块文档
│   ├── ai-podcast/      → AI 播客模块文档
│   ├── novel/           → 有声书模块文档
│   └── ttv2/            → TTS 2.0 文档
│
├── design/              → UI 设计稿 (HTML 原型)
└── CLAUDE.md            → AI 辅助开发上下文文件
```

---

## 🔑 核心设计决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| 注册方式 | 邀请码制（一次有效）| 内测期控制用户规模，防刷号 |
| LLM 多供应商 | 三级瀑布降级链 | 单供应商故障不影响用户体验 |
| 支付可靠性 | RSA2 验签 + DB CAS 幂等 + @Transactional | 防重复扣款、防伪造回调 |
| AI 音乐歌名 | 与歌词同一次 LLM 调用（TITLE: 格式）| 节省 API 调用，保证歌名与歌词风格一致 |
| WebSocket 鉴权 | URL Query Param 传 Sa-Token | WebSocket 协议不支持自定义 Header |
| 前端性能 | keep-alive + IntersectionObserver + v-memo + 节流 | 减少重渲染，首屏加快约 30% |
| AI 接口超时 | 各接口独立超时（10s 全局 / AI 120s）| 避免 AI 长耗时被全局超时截断 |
| 部署 | 新加坡 VPS + Cloudflare Pages + Jenkins CI/CD | 前端全球 CDN，后端海外免备案 |

---

## 🌐 部署信息

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | `https://www.joyoai.xyz` | Cloudflare Pages（全球 CDN）|
| API | `https://joyoai.xyz/api` | Nginx → Spring Boot 9090 |
| 后端服务器 | 新加坡 VPS | 海外服务器，免 ICP 备案 |

---

## 📄 License

Private — SoundRead Team