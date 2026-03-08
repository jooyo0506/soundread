# 📦 SQL 脚本管理

本目录统一管理所有数据库脚本，按功能分为三类。

## 目录结构

```
sql/
├── init/           # 建表 + 初始化数据脚本（按执行顺序排列）
├── migration/      # 增量迁移脚本（ALTER TABLE 等）
├── data/           # 独立的数据填充脚本
└── README.md
```

## 执行顺序

> 首次初始化数据库时，按以下顺序执行 `init/` 目录下的脚本：

| 顺序 | 文件 | 说明 |
|------|------|------|
| 1 | `V1__core_schema.sql` | 核心表：用户、订单、TTS任务、音色、作品、AI交互等 |
| 2 | `V2__novel_schema.sql` | 有声书模块：创作记录、存储计量、小说项目/章节/分段 |
| 3 | `V3__studio_schema.sql` | AI 创作工坊：模板、项目、分段 + 8种创作类型初始数据 |
| 4 | `V4__prompt_library.sql` | AI 指令库：分类表、角色设定 + 初始情感配音指令数据 |
| 5 | `V5__music_schema.sql` | AI 音乐模块：work 表扩展 + music_task 表 |

> 之后按版本号顺序执行 `migration/` 和 `data/` 目录下的脚本：

| 版本 | 文件 | 说明 |
|------|------|------|
| V6 | `migration/V6__music_stream_url.sql` | music_task 添加 stream_url 字段 |
| V7 | `migration/V7__lyric_timings.sql` | music_task 添加 lyric_timings 字段 |
| V8 | `data/V8__tts2_voices_insert.sql` | 插入 Seed-TTS 2.0 大模型音色数据 |
| V9 | `migration/V9__add_section_summary.sql` | studio_section 添加 summary 字段 |
