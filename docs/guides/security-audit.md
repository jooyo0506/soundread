# 06 — 后端安全审计与漏洞修复

> 版本: v1.0 | 审计日期: 2026-02-27 | 审计范围: 16 个 Controller + Sa-Token 配置

---

## 一、审计概览

| 维度 | 审计结果 |
|:-----|:---------|
| 扫描范围 | 16 个 REST Controller, 1 个 Sa-Token 全局配置 |
| 发现漏洞 | **6 类**, 涉及 7 个文件 |
| 风险等级 | 🔴 高危 2 个, 🟠 中危 3 个, 🟡 低危 1 个 |
| 修复状态 | ✅ 全部修复 |

---

## 二、漏洞详情与修复

### 🔴 漏洞 1: IDOR — 创作项目越权访问（高危）

**问题描述**: `StudioService.getProject()` 仅执行 `selectById(projectId)` 无任何归属校验。攻击者只需猜测/遍历项目 ID，即可读取、修改、发布**任何用户**的创作项目。

**影响范围**: 覆盖 8 个 Controller 端点：
- `GET /api/studio/projects/{id}` — 读取他人项目
- `PUT /api/studio/projects/{id}` — 修改他人标题/大纲
- `POST /api/studio/projects/{id}/generate` — 用他人项目调 AI
- `POST /api/studio/projects/{id}/publish` — 把他人项目发布到发现页
- `GET /api/studio/projects/{id}/sections` — 读取他人段落
- `POST /api/studio/sections/{id}/rewrite` — 改写他人段落
- `POST /api/studio/projects/{id}/outline` — 生成他人大纲

**攻击方式**: 
```bash
# 直接用 curl 猜 ID
curl -H "Authorization: Bearer <我的token>" \
     http://localhost:8080/api/studio/projects/2026229317599551490
```

**修复方案** — 在 Service 层加归属校验:
```java
// StudioService.java — 修复后
public StudioProject getProject(Long projectId) {
    StudioProject project = projectMapper.selectById(projectId);
    if (project == null) throw new RuntimeException("项目不存在");
    Long currentUserId = StpUtil.getLoginIdAsLong();
    if (!project.getUserId().equals(currentUserId)) {
        throw new RuntimeException("无权访问该项目");
    }
    return project;
}
```

**设计决策**: 选择在 Service 层而非 Controller 层校验，因为 `getProject()` 被多个方法调用（generate / rewrite / outline / publish），一处修复覆盖所有调用链。

---

### 🔴 漏洞 2: VIP 音色权限绕过（高危）

**问题描述**: `TtsController.shortTextSynthesize()` 在校验音色权限时 **hardcode** 传入 `"tts-1.0"` 引擎参数：

```java
// 修复前 — hardcode "tts-1.0" → 全部 return true
voiceService.checkUserVoicePermission(userId, voiceId, "tts-1.0");
```

而 `checkUserVoicePermission()` 对 `tts-1.0` 直接 `return true`（短文本免费策略）。导致：**任何用户使用任何 VIP 音色调 `/api/tts/short` 都不会被拦截**。

**攻击方式**:
```bash
# 普通用户直接使用 VIP 专属音色（如情感音色）
curl -X POST http://localhost:8080/api/tts/short \
  -H "Authorization: Bearer <普通用户token>" \
  -d '{"text":"测试","voiceId":"BV700_streaming"}'
# → 应返回 "无权使用" 但实际成功合成
```

**修复方案** — 动态检测音色所属引擎:
```java
// TtsController.java — 修复后
String engine = voiceService.detectVoiceEngine(req.getVoiceId());
if (!voiceService.checkUserVoicePermission(userId, voiceId, engine)) {
    return Result.fail("无权使用该音色，请升级VIP或购买");
}

// VoiceServiceImpl.java — 新增方法
public String detectVoiceEngine(String voiceId) {
    SysVoice voice = sysVoiceMapper.selectOne(
        new QueryWrapper<SysVoice>().eq("voice_id", voiceId));
    if (voice == null) return "tts-1.0";
    String engines = voice.getSupportedEngines();
    // 含 tts-2.0 → 走 VIP 校验链路
    if (engines != null && engines.contains("tts-2.0")) return "tts-2.0";
    return "tts-1.0";
}
```

**面试话术**: 根据 `sys_voice.supported_engines` 字段动态判定引擎归属，遵循**最高权限原则** — 如果音色同时属于 tts-1.0 和 tts-2.0，按 tts-2.0 走 VIP 校验，确保权限不被降级旁路。

---

### 🟠 漏洞 3: Sa-Token 路由保护遗漏（中危）

**问题描述**: `SaTokenConfig` 的 `SaRouter.match()` 只保护了部分路由，遗漏了：
- `/api/creation/**` — 创作中心（含创作记录删除、发布）
- `/api/novel/**` — 有声书项目（含项目创建、Pipeline 启动）

**风险**: 未登录用户可直接调用这些接口（虽然内部代码会报 `StpUtil.getLoginIdAsLong()` 异常，但异常信息可能泄露内部结构）。

**修复方案**:
```java
// SaTokenConfig.java — 修复后
SaRouter.match("/api/tts/**", "/api/podcast/**", "/api/voice/**",
    "/api/vip/**", "/api/studio/**", "/api/creation/**",
    "/api/novel/**", "/api/discover/works/*/like")
    .check(r -> StpUtil.checkLogin());
```

---

### 🟠 漏洞 4: 段落操作无归属校验（中危）

**问题描述**: `StudioController` 的 `POST /sections` 和 `DELETE /sections/{id}` 未验证段落所属项目是否归属当前用户。

**攻击方式**: 用户 A 如果知道用户 B 的段落 ID，可以直接修改其内容或删除。

**修复方案**:
```java
// StudioController.java — 修复后
@PostMapping("/sections")
public Result<Long> saveSection(@RequestBody StudioSection section) {
    studioService.checkSectionOwnership(section); // 校验归属
    studioService.saveSection(section);
    return Result.ok(section.getId());
}

@DeleteMapping("/sections/{id}")
public Result<Void> deleteSection(@PathVariable Long id) {
    StudioSection section = studioService.getSectionById(id);
    if (section == null) return Result.fail("段落不存在");
    studioService.checkSectionOwnership(section); // 校验归属
    studioService.deleteSection(id);
    return Result.ok();
}
```

`checkSectionOwnership` 内部调用 `getProject(section.getProjectId())`，复用归属校验逻辑。

---

### 🟠 漏洞 5: 有声书章节分段越权（中危）

**问题描述**: `NovelController.segments()` 未做用户校验：

```java
// 修复前 — 任何登录用户可通过 chapterId 查看他人的章节分段
@GetMapping("/chapter/{chapterId}/segments")
public Result<List<NovelSegment>> segments(@PathVariable Long chapterId) {
    return Result.ok(novelService.listSegments(chapterId));
}
```

**修复方案**: 通过章节反查项目，校验项目归属：
```java
// NovelService.java — 新增
public void checkChapterOwnership(Long chapterId, Long userId) {
    NovelChapter chapter = chapterMapper.selectById(chapterId);
    if (chapter == null) throw new BusinessException("章节不存在");
    getDetail(chapter.getProjectId(), userId); // 触发归属校验
}
```

---

### 🟡 漏洞 6: 前端 SSE Token 泄露风险（低危）

**问题描述**: SSE 请求使用 `fetch()` 并在 Header 中传 Bearer Token，但之前 `localStorage` 的 key 用了错误的 `token` 而非 `sr_token`，导致 SSE 请求无 Token → 被 Sa-Token 拦截 → 前端无反馈（之前已修复）。

---

## 三、安全防护架构总结

```
┌────────────────────────────────────────────────────────┐
│  Layer 1: Sa-Token 全局拦截器 (SaTokenConfig)            │
│  • /api/tts/**, /api/studio/**, /api/creation/**       │
│  • /api/novel/**, /api/podcast/**, /api/voice/**       │
│  └→ 未匹配路径: 白名单放行 (auth/**, discover/works)     │
├────────────────────────────────────────────────────────┤
│  Layer 2: Controller 层功能权限                          │
│  • @RequireFeature("ai_script") — TtsV2Controller      │
│  • tierPolicyService.hasFeature() — NovelController     │
├────────────────────────────────────────────────────────┤
│  Layer 3: Service 层数据归属校验                          │
│  • StudioService.getProject() — userId 比对              │
│  • StudioService.checkSectionOwnership() — 段落→项目链   │
│  • NovelService.getDetail() — userId 比对                │
│  • NovelService.checkChapterOwnership() — 章节→项目链    │
├────────────────────────────────────────────────────────┤
│  Layer 4: 业务权限校验                                   │
│  • VoiceService.detectVoiceEngine() — 音色引擎动态判定   │
│  • VoiceService.checkUserVoicePermission() — 4级权限链   │
│  • QuotaService — 配额校验 (字数/AI次数/存储)             │
└────────────────────────────────────────────────────────┘
```

---

## 四、修改文件清单

| 文件 | 变更类型 | 修复漏洞 |
|:----|:--------|:---------|
| `StudioService.java` | 修改 `getProject()` + 新增 `checkSectionOwnership()` + `getSectionById()` | #1, #4 |
| `StudioController.java` | `saveSection` / `deleteSection` 加归属校验 | #4 |
| `TtsController.java` | hardcode `"tts-1.0"` → `detectVoiceEngine()` | #2 |
| `VoiceService.java` | 新增 `detectVoiceEngine()` 接口 | #2 |
| `VoiceServiceImpl.java` | 实现 `detectVoiceEngine()` | #2 |
| `SaTokenConfig.java` | 补 `/api/creation/**`, `/api/novel/**` 路由保护 | #3 |
| `NovelController.java` | `segments()` 加 `checkChapterOwnership` | #5 |
| `NovelService.java` | 新增 `checkChapterOwnership()` | #5 |

---

## 五、面试亮点提炼

> **Q: 项目中遇到过安全问题吗？怎么处理的？**

1. **发现方式**: 在开发 AI 创作模块（TTS 2.0 音色引擎切换）时，发现 `TtsController` 的权限检查写死了 `"tts-1.0"` 引擎参数，导致 VIP 音色可被任何用户绕过。由此触发了**全量 16 个 Controller 的安全审计**。

2. **审计方法**: 系统性扫描所有 `@RestController` → 检查 4 个维度：
   - **认证**: Sa-Token 路由覆盖是否完整
   - **授权**: IDOR (Insecure Direct Object Reference) 越权风险
   - **业务权限**: VIP/配额等能否被绕过
   - **输入验证**: XSS/注入风险

3. **核心发现**: 6 类漏洞，其中最严重的是 **IDOR 越权** — `StudioService.getProject()` 无归属校验，影响 8 个 API 端点。

4. **修复策略**: **Defense in Depth（纵深防御）** — 4 层递进保护:
   - L1: Sa-Token 全局登录拦截（网关层）
   - L2: `@RequireFeature` 功能权限注解（权限层）
   - L3: Service 层 userId 归属校验（数据层）
   - L4: 音色引擎动态检测 + 配额扣减（业务层）

5. **设计决策**: 归属校验放在 **Service 层**而非 Controller 层，因为 `getProject()` 被 `generate / rewrite / publish / outline` 等多个方法复用，一处修复自动覆盖整条调用链，避免遗漏。

---

## 六、新增发现（2026-03-14 追加）

### 🟠 `@RequireFeature` AOP 与 SSE 端点不兼容（中危）

**问题描述**: 在调试广播剧功能不可用（"0 字符"输出）时发现，`@RequireFeature("ai_drama")` 注解在 SSE（`text/event-stream`）端点上行为异常。

**技术机制**:
1. AOP 切面拦截到未开通功能后抛出 `BusinessException`
2. Spring 全局异常处理器 `@ControllerAdvice` 将异常序列化为 JSON（`application/json`）
3. 但该端点 `@PostMapping(produces = TEXT_EVENT_STREAM_VALUE)` 声明返回 `text/event-stream`
4. Spring MVC 发现 JSON 响应与 SSE 声明不匹配，内容协商失败
5. 最终返回空的 405 / 406 响应，前端无法感知真实错误

**影响范围**: 所有使用 `@RequireFeature` + `SseEmitter` 的端点。

**修复原则**: **SSE 流式端点绝对不能使用 AOP 注解做权限拦截**。权限检查必须在方法体内部完成，错误通过流事件（`emitter.send([ERROR_MARKER]msg)`）传递给前端。

**已修复端点**:
- `StudioController.generateDrama()` — 改用 `tierPolicyService.hasFeature()` 内联检查
- 其他 SSE 端点（`generateContent`、`rewriteSection`）本已采用内联模式，无需修改

