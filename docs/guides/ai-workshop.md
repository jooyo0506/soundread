# AI 声音工坊 — 优化前后问题总结

> 本次优化时间：2026-03-07 凌晨 2:30 ~ 3:36

---

## 一、Agent 乱调工具

### 问题现象
用户输入"4"（想查看作品），Agent 却自己编了一段"科技产品测评"内容并调用 synthesizeSpeech 合成了语音，完全答非所问。

### 根因分析
1. SystemMessage 风格是"少问多做，直接出活"，导致 LLM 过度积极，没有文字也强行合成
2. 欢迎消息用无序符号 `•` 列出功能，Agent 不知道"4"对应哪个选项
3. 没有任何规则约束"什么时候不该调工具"

### 优化方案
| 层级 | 措施 | 文件 |
|------|------|------|
| 前端第一层 | 首条消息的数字输入前端直接拦截，本地回复引导 | `AiWorkshop.vue` |
| 后端第二层 | SystemMessage 加入 ReAct 思考框架 + 严禁规则 | `SmartAssistant.java` |

### 优化前后对比
| 输入 | 优化前 | 优化后 |
|------|--------|--------|
| "1" | ❌ 直接合成语音 | ✅ 本地引导："你想写什么主题的台本？" |
| "2" | ❌ 直接合成语音 | ✅ 本地引导："请发文字给我合成" |
| "4" | ❌ 合成"科技产品测评" | ✅ 发给 Agent → 调用 listMyWorks |

---

## 二、编号拦截过度

### 问题现象
Agent 给出 3 种台本风格（1.幽默 2.温暖 3.沙雕），用户输入"1"想选第 1 种，但前端拦截了，弹出"你想写什么主题？"，完全脱离对话。

### 根因分析
前端的编号拦截逻辑不区分场景，任何时候输入数字都会被拦截。

### 优化方案
加 `isFirstMessage` 判断 — 只有用户之前没发过消息时才拦截数字，有对话上下文时正常发给 Agent。

### 优化前后对比
| 场景 | 优化前 | 优化后 |
|------|--------|--------|
| 欢迎页输入"1" | ✅ 前端拦截引导 | ✅ 前端拦截引导（不变） |
| Agent 给了编号选项后输入"1" | ❌ 前端错误拦截 | ✅ 发给 Agent 理解上下文 |

---

## 三、默认 voiceId 无效导致重试链

### 问题现象
每次合成语音，第一次必定失败，然后 Agent 调 listVoices 找到正确音色再重新合成，整个链路耗时 ~50 秒。

### 根因分析
SystemMessage 里写的默认 voiceId `zh_female_shuangkuaisisi_moon_bigtts` 在数据库 `sys_voice` 表里不存在。

### 优化方案
从数据库查出真实音色 ID，改为 `zh_female_vv_uranus_bigtts`（vivi 2.0 女声）。

### 优化前后对比
| | 优化前 | 优化后 |
|--|--------|--------|
| 第一次合成 | ❌ 失败 → 重试 | ✅ 直接成功 |
| 完整链路耗时 | ~50 秒 | ~30 秒 |
| LLM 调用次数 | 4-5 次 | 2-3 次 |

---

## 四、超时导致请求失败

### 问题现象
Agent 完整工作流（LLM 决策 → 生成台本 → 再次 LLM → 合成语音）经常超时报错。

### 根因分析
1. 后端 OpenAiChatModel 超时只有 30 秒，多步 Agent 流程轻易超限
2. 前端 axios 默认超时也不够

### 优化方案
| 位置 | 优化前 | 优化后 |
|------|--------|--------|
| 后端模型 timeout | 30 秒 | 60 秒 |
| 前端请求 timeout | 默认值 | 120 秒 |

---

## 五、等待体验差

### 问题现象
用户发送消息后，只看到一个转圈和"思考中…"，等 30-60 秒没有任何反馈，以为卡死了。

### 优化方案 — 分阶段 Loading 提示
```
0s   → "正在理解你的需求..."
5s   → "正在创作台本..."
12s  → "正在选择最佳音色..."
20s  → "正在合成语音，请稍候..."
40s  → "还在努力中，马上就好..."
```

---

## 六、合成后音频播放延迟

### 问题现象
合成完成后播放器出现了，用户点播放还要等 2-5 秒加载音频才有声音。

### 优化方案 — 自动预加载 + 自动播放
```
优化前：渲染播放器 → 用户点播放 → 开始加载 → 等 2-5 秒 → 播放
优化后：渲染播放器 → 同时 new Audio() 后台预加载
                    → canplaythrough → 自动播放 🔊
                    → 2 秒兜底：超时则边缓冲边播
```

---

## 七、UI 交互优化汇总

| 优化项 | 优化前 | 优化后 |
|--------|--------|--------|
| 功能入口 | 纯文字编号"1️⃣ 写台本" | 可点击彩色按钮 `[✍️ 写台本]` |
| 不满意重新生成 | 只能重新输入 | AI 回复底部「🔄 重新生成」按钮 |
| 合成后下一步操作 | 无引导 | 自动出现「换个音色」「重写台本」按钮 |
| 音频播放器 | 只有播放/暂停 | 进度条 + 可拖动定位 + 时间显示 |
| 保存提示 | 无 | 合成后显示"✅ 已保存到创作库" |
| 刷新页面 | 对话丢失 | localStorage 持久化，刷新不丢 |

---

## 八、SystemMessage 演进

| 版本 | 风格 | 问题 |
|------|------|------|
| v1 | "少问多做，直接出活" | Agent 太积极，没文字也强行合成 |
| v2 | 加编号映射（输入"1"→问主题） | 前端已拦截编号，后端映射多余 |
| v3（当前） | **ReAct 思考框架** — 先判断意图再行动 + 严禁规则 | ✅ 稳定 |

### 当前 SystemMessage 核心结构
```
思考框架：先判断意图再行动
├── 说了具体场景 → 生台本 + 合成
├── 发了文字要合成 → 直接合成
├── 发了数字且上文有编号 → 选择选项
├── 问音色/作品 → 对应查询
└── 意图不清楚 → 文字引导，不调工具

严禁规则：
├── 没有文字不得调 synthesizeSpeech
├── 不自己编内容合成
└── 每个工具最多调一次
```

---

## 九、[2026-03-17] Agent SSE 流式输出

### 问题现象
Agent 回复需要 15-22s 同步阻塞，用户只能看到假进度条。输入"你好"也需要 20s。

### 优化方案

| 层级 | 措施 | 文件 |
|------|------|------|
| 后端 | 新增 `StreamingSmartAssistant` 接口返回 `TokenStream` | `StreamingSmartAssistant.java` |
| 后端 | `POST /api/agent/chat-stream` SSE 端点，使用 `OpenAiStreamingChatModel` | `AgentController.java` |
| 前端 | `sendMessage()` 改用 `fetch ReadableStream`，逐 token 更新消息 | `AiWorkshop.vue` |
| 前端 | 保留 sync `/chat` 作为 fallback（SSE 失败时自动降级） | `AiWorkshop.vue` |
| 快速通道 | 简单问候（你好/嗨/hi）跳过 LLM 直接返回预设回复 | `AgentController.java` |
| LLM 调参 | maxTokens 1024→512，maxMessages 20→10 | `AgentController.java` |

### 优化前后对比

| 指标 | 优化前 | 优化后 |
|------|--------|--------|
| "你好" 响应 | ~20s | ~50ms (快速通道) |
| 常规问题首 token | ~15s | < 1s (SSE) |
| 完整回复显示 | 15-22s 后一次性出现 | 逐字实时打字效果 |

---

## 十、[2026-03-17] 重置对话后记忆仍存在

### 问题现象
用户点击"重置"按钮后，重新发消息时 Agent 仍记得之前的对话内容。

### 根因分析
`reset()` 方法的 memoryId 使用 `token.hashCode()`，但 `chat()`/`chatStream()` 使用 `user.getId().toString()`，**两者永远不匹配**。此外 `reset()` 调用 `memoryCache.invalidate()` 清的是 Caffeine cache，而实际记忆存储在 `InMemoryChatMemoryStore` 中。

### 修复方案

| 改动 | 说明 |
|------|------|
| memoryId 统一 | `reset()` 改用 `user.getId().toString()` |
| 共享 memoryStore | `InMemoryChatMemoryStore` 提升为实例字段 `sharedMemoryStore`，sync/streaming 共用 |
| 正确清空 | `reset()` 调用 `sharedMemoryStore.deleteMessages(memoryId)` |

---

## 文件修改清单

| 文件 | 改动 |
|------|------|
| `SmartAssistant.java` | voiceId 修正 + SystemMessage 三版迭代 → ReAct 思考框架 |
| `StreamingSmartAssistant.java` | **新增** 流式 Agent 接口（TokenStream） |
| `AgentController.java` | SSE 流式端点 / 快速通道 / 参数优化 / 共享 memoryStore / reset 修复 |
| `AiWorkshop.vue` | SSE ReadableStream / 实时打字效果 / sync fallback |
| `WebSocketConfig.java` | 移除不存在的 `InteractionWebSocketHandler` |
| `StudioWorkbench.vue` | TTS 并行合成（并发≤3） |
| `tts.js` | 新增 `synthesizeV2()` 统一入口 |

