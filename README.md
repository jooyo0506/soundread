# 🎙️ 声读 SoundRead

> **让文字发声，为内容赋能** — 独立开发的 AI 声音内容创作平台，已面向真实用户上线运营

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue3](https://img.shields.io/badge/Vue-3.4-42b883)](https://vuejs.org/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.30.0-orange)](https://github.com/langchain4j/langchain4j)
[![License](https://img.shields.io/badge/License-Private-red)]()

🌐 **线上地址**：[https://www.joyoai.xyz](https://www.joyoai.xyz)

---

## 📌 项目简介

声读是一个面向内容创作者的一站式 **AI 语音内容平台**，由本人独立设计并开发，从零完成前端、后端、AI 集成和运维部署的全链路工作。

**项目规模**：后端 21 个 Service、16 个 Controller、22 张业务表；前端 15 个页面；已集成 6 家 LLM 供应商、2 代 TTS 协议、AI 音乐生成、RAG 知识库。

---

## ✨ 核心功能模块

| 模块 | 功能描述 | 核心技术 | 状态 |
|------|---------|---------|------|
| 🗣️ **TTS 1.0** | 文字转语音，支持长文本异步合成 | 火山引擎 REST API + 异步任务队列 | ✅ 上线 |
| 🎭 **TTS 2.0 情感合成** | 情感标注 (`#开心` `#悲伤`) + AI 台本生成 | 火山引擎 WebSocket 全双工 + SSE 流式 | ✅ 上线 |
| 🤖 **AI 智能助手** | 对话式 AI，可调用工具完成查询/创作/合成 | LangChain4j Tool Calling + SSE 流式 | ✅ 上线 |
| 📝 **AI 创作工坊** | 8 种创作类型，含短剧/播客/电台/带货文案 | Agent Pipeline + LLM 摘要压缩记忆链 | ✅ 上线 |
| 🎵 **AI 音乐** | 歌词+歌名+旋律同步生成，HLS 流式播放 | Mureka API + HLS.js + 毫秒级歌词同步 | ✅ 上线 |
| 🎙️ **AI 播客** | 双人对话播客，实时 WebSocket 流式生成 | WebSocket + 双人 TTS 合成流水线 | ✅ 上线 |
| 📖 **有声书** | 长篇小说 → 有声书，四段式异步 Pipeline | 分章→分段→情感标注→TTS 合成 | ✅ 上线 |
| 🔒 **邀请码注册** | 内测阶段防刷号用户增长管控 | 一次性邀请码 + 支付宝扫码开通 VIP | ✅ 上线 |
| 🔍 **RAG 知识库** | Agent 回答产品问题时基于真实文档检索 | pgvector + AllMiniLmL6V2 本地 Embedding | ✅ 上线 |
| 📄 **草稿自动保存** | AI 对话完成后自动保存草稿，防内容丢失 | SSE onComplete 非阻塞写入 MySQL | ✅ 上线 |

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│  ┌──────────────┐   ┌─────────────────┐   ┌──────────────────┐  │
│  │ web/ (Vue3)  │   │ admin/ (Vue3)   │   │ design/ (原型)   │  │
│  │  15 pages    │   │  运营后台 3页    │   │  8 HTML mockups  │  │
│  └──────┬───────┘   └───────┬─────────┘   └──────────────────┘  │
│         │  REST / SSE / WebSocket          │                     │
├─────────┼──────────────────────────────────┼─────────────────────┤
│         ▼                  ▼             server/ (Spring Boot 3) │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  API Gateway：RateLimit AOP + JWT 鉴权 + 全局异常处理        │  │
│  └─────────────────────────┬──────────────────────────────────┘  │
│                            ▼                                     │
│  ┌───────────────┐  ┌─────────────────────────────────────────┐  │
│  │  AI Agent     │  │  Business Service (21)                   │  │
│  │  LangChain4j  │  │  TTS / Music / Novel / Studio / Auth...  │  │
│  │  Tool Calling │  └─────────────────────────────────────────┘  │
│  │  + RAG        │                    │                          │
│  └───────┬───────┘  ┌─────────────────▼─────────────────────┐   │
│          │          │  Adapter Layer                          │   │
│          │          │  LlmRouter / R2Storage / TTS / ASR      │   │
│          │          └───────────────────────────────────────-─┘   │
│          │                                                        │
│  ┌───────▼───────────────────────────────────────────────────┐   │
│  │  Infrastructure                                            │   │
│  │  MySQL 8 │ Redis 7 │ Cloudflare R2 │ pgvector │ RocketMQ  │   │
│  └───────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.2.5 |
| **前端框架** | Vue 3 + Vite + Pinia | 3.4 |
| **AI Agent** | LangChain4j | 0.30.0 |
| **认证** | Sa-Token + JWT | 1.38.0 |
| **ORM** | MyBatis-Plus | 3.5.6 |
| **数据库** | MySQL 8 / Redis 7 / PostgreSQL+pgvector | — |
| **对象存储** | Cloudflare R2（S3 兼容） | — |
| **TTS** | 火山引擎 v1.0 REST + v2.0 WebSocket | — |
| **LLM** | 豆包/DeepSeek/Qwen/GLM/MiniMax/Gemini | 三级瀑布降级路由 |
| **AI 音乐** | Mureka API | — |
| **支付** | 支付宝 SDK | RSA2 签名 |
| **CI/CD** | Jenkins + Nginx | 新加坡 VPS |

---

## 🔧 工程亮点

### 1. LLM 多供应商三级瀑布降级

接入 6 家 LLM 供应商，通过 `LlmRouter` 实现三级瀑布降级——用户绑定模型 → 系统基线模型 → 最终兜底，任意单一供应商故障用户无感知自动切换。

```java
// 三级降级路由示意
Level 1: 用户配置的首选模型（豆包/DeepSeek 等）
Level 2: 系统稳定基线（Qwen / GLM / Gemini）
Level 3: 最终兜底模型
```

### 2. AI Agent Tool Calling + SSE 流式输出

使用 LangChain4j 实现支持 8 个工具函数的 ReAct Agent，解决了流式场景下 `ThreadLocal` 上下文丢失的并发问题：

- **问题**：OkHttp 线程池处理 SSE 回调时，`ThreadLocal` 中的用户上下文无法跨线程传递
- **解决**：将 `SoundReadTools` 从 Singleton 改为请求级实例，通过拷贝构造函数显式传递 `User` 对象，彻底解耦线程绑定依赖

### 3. RAG 产品知识库（本地 Embedding，零 API 成本）

- 使用 AllMiniLmL6V2（ONNX）本地模型生成 384 维 Embedding，无需调用 Embedding API
- 向量存入 pgvector，`@PostConstruct` 自动检测并一次性导入
- RAG 失败时降级无增强模式，不阻断主流程

### 4. 支付安全：RSA2 验签 + CAS 幂等防重

```java
// 支付宝异步回调处理：三重保障
1. RSA2 验签：防伪造回调
2. CAS 原子操作：UPDATE order SET status=2 WHERE id=? AND status=1
3. @Transactional：保证积分发放与状态变更原子性
```

### 5. ChatMemory Caffeine 防 OOM

原生 `InMemoryChatMemoryStore` 无上限、无过期，生产环境存在 OOM 风险。替换为 Caffeine 支撑的自定义实现：maximumSize(500) + expireAfterAccess(30min) + LRU 淘汰。

### 6. AI 工作坊渐进式加载 UX

SSE 流式场景首 Token 延迟可达 10-20 秒，通过意图识别 + 管线动画提升感知性能：
- 根据用户消息关键词判断意图（查询/情感/合成/通用）
- 不同意图展示不同的阶段动画（"AI 正在创作台本..." → "正在合成语音..."）
- 首 Token 到达时停止动画，切换为实时打字效果

---

## 🔑 核心设计决策

| 决策点 | 方案选型 | 理由 |
|--------|---------|------|
| LLM 多供应商 | 三级瀑布降级路由 | 单供应商故障不损害用户体验 |
| 流式 Agent 并发安全 | 请求级工具实例 + 显式传参 | 彻底解耦 ThreadLocal，比 InheritableThreadLocal 更安全 |
| ChatMemory 存储 | Caffeine 替代 InMemoryStore | 有上限 + TTL + LRU，防生产 OOM |
| 支付幂等 | CAS 状态机 + 数据库乐观锁 | 防止重复发放权益 |
| RAG Embedding | 本地 ONNX 模型 | 零 API 费用，冷启动 ~1s，推理 <100ms |
| 注册方式 | 邀请码制 | 内测阶段控制用户规模，防刷号 |
| 部署方案 | 新加坡 VPS + Cloudflare Pages | 海外服务器免备案，前端全球 CDN 加速 |
| WebSocket 鉴权 | URL Query Param 传 Token | WebSocket 握手不支持自定义 Header |
| 前端性能 | keep-alive + IntersectionObserver + 节流 | 首屏加快约 30%，长列表无卡顿 |

---

## 🚀 本地启动

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.x |
| Redis | 6+ |
| PostgreSQL + pgvector | 15（RAG 功能可选） |

### 启动步骤

```bash
# 1. 克隆项目
git clone <repo-url> && cd soundread

# 2. 初始化数据库（顺序执行 V1–V14）
mysql -u root -p < docs/sql/init/V1__core_schema.sql
# ... 其余迁移脚本见 docs/sql/

# 3. 配置环境变量
cp server/src/main/resources/application-example.yml \
   server/src/main/resources/application.yml
# 填入：LLM API Keys / 火山引擎 / 支付宝 / Redis / DB 等

# 4. 后端
cd server && mvn clean package -DskipTests
java -jar target/soundread-server-1.0.0-SNAPSHOT.jar

# 5. 前端
cd web && pnpm install && pnpm dev
```

---

## 📁 项目结构

```
soundread/
├── server/                   → Spring Boot 后端
│   └── src/main/java/com/soundread/
│       ├── adapter/          → 外部服务适配（LLM/R2/TTS/ASR）
│       ├── agent/            → AI Agent（creative/emotion/drama/toolcalling/rag）
│       ├── common/           → 统一响应 + AOP 限流 + 全局异常处理
│       ├── config/           → Sa-Token/R2/WebSocket/LLM路由/Redis 配置
│       ├── controller/       → REST 控制器（16 + 2子模块）
│       ├── job/              → 定时任务（热度计算/数据清理）
│       ├── mapper/           → MyBatis-Plus Mapper（22张表）
│       ├── service/          → 业务层（21 Service）
│       └── websocket/        → WebSocket Handler
│
├── web/                      → Vue 3 用户端（15 页面）
│   └── src/
│       ├── api/              → 10 个 API 模块（AI 接口独立超时）
│       ├── stores/           → 3 个 Pinia Store
│       └── views/            → AiWorkshop / Discover / Studio 等
│
├── admin/                    → Vue 3 运营后台（3 页面）
├── docs/                     → 技术文档 + SQL 迁移脚本
│   ├── sql/                  → V1–V14 建表与迁移脚本
│   └── guides/               → 部署/性能/安全/配额等技术指南
└── design/                   → HTML 原型设计稿
```

---

## 🌐 线上部署

| 服务 | 地址 | 说明 |
|------|------|------|
| 用户前端 | [https://www.joyoai.xyz](https://www.joyoai.xyz) | Cloudflare Pages 全球 CDN |
| API 服务 | `https://joyoai.xyz/api` | Nginx 反向代理 → Spring Boot 9090 |
| 后端服务器 | 新加坡 VPS | 海外部署，免 ICP 备案 |
| CI/CD | Jenkins | push 自动触发构建部署 |

---

## 📄 License

Private — SoundRead 个人项目，仅供学习与展示。