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
| 免费用户 | ¥0 | 每日 2000 字基础 TTS、5 次 AI 互动 |
| VIP 月度 | ¥30/月 | 5 万字/天、情感合成、AI 播客、创作工坊、音乐生成 |
| VIP 年度 | ¥300/年 | 无限字数、50 个创作项目、200+ 创作历史 |
| VIP 终身 | ¥1000 | 全部无限 |

---

## ✨ 功能矩阵

| 模块 | 功能 | 技术亮点 |
|------|------|---------|
| 🗣️ TTS 1.0 | 短文本/长文本语音合成 | 火山引擎 REST + 异步任务 |
| 🎭 TTS 2.0 | 情感合成 + AI 台本 | WebSocket 全双工 + 情感指令 (`#开心` `#悲伤`) |
| 🎬 AI 短剧 | 多角色对话 + TTS 合成 | 角色解析 + 分角色音色绑定 |
| 🎙️ AI 播客 | 双人对话式播客 | LLM 多轮生成 + 双人播客 TTS |
| 🎵 AI 音乐 | 歌曲/纯音乐/歌词生成 | Mureka API + HLS 流式播放 + 歌词同步 |
| 📝 AI 创作工坊 | 8 种创作类型 | Template Method 模式 + 摘要压缩记忆链 |
| 📖 有声书 | 长篇小说 → 有声书 | 4 阶段异步 Pipeline (分章→分段→标注→合成) |
| 🗣️ 声音克隆 | 上传样本 → 个人音色 | Whisper 质检 + 火山引擎克隆 API |
| 💬 边听边问 | 收听时语音提问 | ASR → LLM → TTS 全链路语音闭环 |
| 🤖 AI 助手 | Tool Calling 智能助手 | LangChain4j Agent + 8 个工具函数 |

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
│   │  MySQL 8 │ Redis │ R2 │ RocketMQ │ 火山TTS │ 豆包LLM  │   │
│   └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| **核心框架** | Spring Boot | 3.2.5 |
| **前端框架** | Vue 3 + Vite + Pinia | Vue 3.4 |
| **认证鉴权** | Sa-Token + JWT | 1.38.0 |
| **ORM** | MyBatis-Plus | 3.5.6 |
| **数据库** | MySQL | 8.x |
| **缓存** | Redis | 7.x |
| **对象存储** | Cloudflare R2 (S3 兼容) | — |
| **消息队列** | RocketMQ | 2.3.0 |
| **AI Agent** | LangChain4j | 0.30.0 |
| **LLM** | 豆包/DeepSeek/Qwen/MiniMax | 多供应商 |
| **TTS** | 火山引擎 v1.0 + v2.0 | WebSocket 双向流 |
| **AI 音乐** | Mureka API | — |
| **ASR** | Whisper | v1 |

---

## 🚀 快速启动

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| pnpm | 8+ |
| MySQL | 8.x |
| Redis | 6+ |

### 启动步骤

```bash
# 1. 克隆项目
git clone <repo-url>
cd soundread

# 2. 初始化数据库 (按顺序执行)
mysql -u root -p < docs/sql/init/V1__core_schema.sql
mysql -u root -p < docs/sql/init/V2__novel_schema.sql
mysql -u root -p < docs/sql/init/V3__studio_schema.sql
mysql -u root -p < docs/sql/init/V4__prompt_library.sql
mysql -u root -p < docs/sql/init/V5__music_schema.sql

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
│       ├── common/      → 统一响应体 + AOP + 异常处理
│       ├── config/      → 配置 (Sa-Token/R2/WebSocket/AI路由)
│       ├── controller/  → REST 控制器 (16 + 2 子模块)
│       ├── entity/      → 扩展实体 (Novel/Voice/Creation/Music)
│       ├── job/         → 定时任务 (热度计算/数据清理)
│       ├── mapper/      → MyBatis-Plus Mapper (21 张表)
│       ├── model/       → 数据模型 (13 Entity + 7 DTO)
│       ├── sdk/         → 底层 SDK 封装
│       ├── service/     → 业务逻辑 (21 Service)
│       └── websocket/   → WebSocket (播客/边听边问)
│
├── web/                 → Vue 3 用户端
│   └── src/
│       ├── api/         → 10 个 API 模块
│       ├── components/  → 5 个全局组件
│       ├── composables/ → WebSocket 组合式函数
│       ├── stores/      → 3 个 Pinia Store
│       └── views/       → 15 个页面
│
├── admin/               → Vue 3 运营后台
│   └── src/
│       ├── api/         → 3 个 API 模块
│       ├── components/  → AdminLayout
│       ├── stores/      → 2 个 Pinia Store
│       └── views/       → 3 个页面 (Works/Policy/Login)
│
├── docs/                → 技术文档
│   ├── sql/             → 统一 SQL 脚本 (init/migration/data)
│   ├── ai-music/        → AI 音乐模块文档
│   ├── ai-podcast/      → AI 播客模块文档
│   ├── novel/           → 有声书模块文档
│   └── ttv2/            → TTS 2.0 文档
│
├── design/              → UI 设计稿 (HTML 原型)
└── data/                → 测试音频
```

---

## 📄 License

Private — SoundRead Team