# AI 小说创作工作流技术文档

> 更新日期：2026-03-01

## 架构概览

```
前端 StudioWorkbench.vue
  ├── Step 1: 灵感构思 → 选题材 + 输入灵感 + 章数
  ├── Step 2: 大纲规划 → AI 生成：梗概 + 角色 + 每章详细大纲(plot/keyEvents/foreshadowing)
  └── Step 3: 逐章创作 → 根据大纲生成正文 → 阅读 → 完结发布

后端 StudioService.java
  ├── generateContent() → 构建 System Prompt + RAG 记忆注入 + SSE 流式输出
  ├── saveGeneratedContent() → 解析章节号 + 保存 section + 生成摘要 + RAG 索引
  └── NovelAgent.java → 角色定义 + 输出格式规则 + maxOutputTokens 限制
```

## 3 步创作流程

### Step 1: 灵感构思 (`novelStep = 'conceive'`)

- 用户选择**题材方向**（7 种热门题材）
- 输入**创作灵感**
- 选择**目标章数**（5/10/20 章）

### Step 2: 大纲规划 (`novelStep = 'outline'`)

AI 一次性生成完整规划，返回 JSON：

```json
{
  "synopsis": "200字以内的故事梗概",
  "characters": [{"name": "角色名", "desc": "性格/身份描述"}],
  "chapters": [
    {"plot": "详细剧情(50-100字)", "keyEvents": "核心事件", "foreshadowing": "伏笔"},
    ...
  ]
}
```

- 每章以可折叠卡片展示，支持编辑 plot/keyEvents/characters/foreshadowing
- JSON 解析策略：策略1 直接 `JSON.parse`，策略2 正则提取
- 支持对象数组（新格式）和字符串数组（兼容旧格式）
- 点击 **"确认大纲 → 开始创作"** 直接进入 Step 3

### Step 3: 逐章创作 (`novelStep = 'generate'`)

- 逐章生成，每章生成后**锁定不可修改**
- 锁定章节可点击 **📖 展开阅读** 查看全文
- 进度条使用 `getValidSection(i)` 统一校验
- 全部完成后显示**完结卡片**：统计、阅读全文、复制、完结发布

## 上下文记忆架构

每次生成一章时，后端自动注入两层记忆：

| 层级 | 机制 | 内容 | Token 消耗 |
|------|------|------|-----------|
| **摘要记忆** | `sectionMemory.buildSummaryMemory()` | 每章 ≤50 字 AI 摘要拼接 | ~250 (5章) |
| **RAG 向量** | `sectionMemory.retrieveRelevantContext()` | 3+章后启用，语义搜索相关段落 | ~300 |
| **全局设定** | System Prompt | 角色定义 + 梗概 + 角色表 | ~500 |
| **当前章细纲** | 前端构建 | plot + keyEvents + 用户补充 | ~150 |

## 字数控制

| 位置 | 设定 |
|------|------|
| 后端 `NovelAgent.getOutputFormatRule()` | "正文严格控制在 1000-2000 字之间！绝对不允许超过 2000 字！" |
| 后端 `NovelAgent.getMaxOutputTokens()` | `2200`（硬截断兜底） |
| 前端 `generateFromOutline` prompt | "1000-2000字，绝对不允许超过2000字" |

## 章节索引修复

**Bug**：`saveGeneratedContent` 之前用 `project.getTotalSections()` 自增作为 sectionIndex，导致生成第1章却保存为第3章。

**修复**：新增 `parseChapterIndex(userInput)` 方法，从 `【章节标题：第X章】` 解析真实章节号，先删旧数据再插入。

```java
private int parseChapterIndex(String userInput) {
    if (userInput == null) return -1;
    Matcher m = Pattern.compile("【章节标题：第(\\d+)章】").matcher(userInput);
    if (m.find()) return Integer.parseInt(m.group(1)) - 1;
    return -1;
}
```

## 内容校验

`isValidChapterContent(content)` 统一检测无效内容：

- `{` / `[{` / `[` 开头 → JSON 数据
- ` ``` ` 开头 → markdown 代码块
- 包含 `"synopsis"` / `"plot"` → JSON 字段
- 长度 < 100 字 → 内容太短

所有需要判断章节是否有效的地方统一调用 `getValidSection(i)`。

## 关键文件

| 文件 | 职责 |
|------|------|
| `frontend/src/views/StudioWorkbench.vue` | 小说创作全部 UI + 逻辑 |
| `backend/.../service/StudioService.java` | 内容生成 + section 保存 + RAG |
| `backend/.../agent/creative/NovelAgent.java` | 角色 prompt + 字数规则 + token 限制 |
| `backend/.../service/SectionMemoryService.java` | 摘要记忆 + 向量检索 |
| `frontend/src/api/studio.js` | 前端 API 层 |

## Agent Workflow 未来方向

```
一键创作 Pipeline (Phase 2):
  InspirationAgent → OutlineAgent → WritingAgent → QualityAgent → PublishAgent
                                         ↑              ↓
                                    风格一致性校验    毒点检测修改
```

每个 Agent 可插拔配置，支持多风格、角色一致性校验、毒点检测等。
