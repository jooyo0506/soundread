# 声读 SoundRead — 问题记录与优化方案

> 最后更新：2026-03-14  
> 状态图例：✅ 已修复 | ⏳ 计划中 | ❌ 待修复

---

## 一、已修复问题清单

### 1.7 SSE fetch 请求域名错误导致 405（前端级 Bug）

**问题**：广播剧一键生成、AI 正文生成、AI 改写等 SSE 流式接口全部返回 405 Method Not Allowed，后端日志完全没有请求记录。

**根因**：
```
前端部署在 www.joyoai.xyz（Cloudflare Pages 静态托管）
axios 配置了 VITE_API_BASE_URL=https://joyoai.xyz/api（正确指向后端）
但 SSE 用 fetch() 写了相对路径 /api/studio/...
→ 浏览器自动拼为 https://www.joyoai.xyz/api/studio/...（错误！）
→ Cloudflare Pages 收到 POST → 返回 405
→ 后端从未被调用
```

**修复（已实施 ✅）**：
```javascript
// studio.js — 统一 SSE fetch 基础 URL
const SSE_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

// 所有 SSE fetch 改用绝对路径
fetch(`${SSE_BASE}/studio/projects/${id}/drama-generate`, { ... })
```

**关键教训**：Cloudflare Pages 前端托管（`www`）和后端 API 域名（无 `www`）不同，axios 自动处理了 baseURL，但原生 `fetch()` 不经过 axios 拦截器，必须手动维护基础 URL。

---

### 1.8 @RequireFeature AOP 与 SSE 端点内容协商冲突

**问题**：广播剧 `generateDrama` 接口被 `@RequireFeature("ai_drama")` AOP 注解拦截时，前端收到空的 405 响应，不显示任何错误提示，后端也无日志。

**根因**：
1. AOP 切面在方法执行**前**抛出异常
2. Spring 全局异常处理器将异常转为 JSON（`Content-Type: application/json`）
3. 但该端点声明返回 `text/event-stream`（SSE）
4. Spring MVC 内容协商失败 → 返回空 405

**修复（已实施 ✅）**：
```java
// 移除 AOP 注解，改用内联判断
// @RequireFeature("ai_drama")  ← 删除
if (!tierPolicyService.hasFeature(user.getTierCode(), "ai_drama")) {
    emitter.send(SseEmitter.event().data("[DRAMA_ERROR]当前套餐未开通广播剧功能"));
    emitter.complete();
    return emitter;
}
```

**设计原则**：**SSE 流式端点不能使用 AOP 注解做权限拦截**，必须在方法体内部检查，通过流事件传递错误信息。与 `TtsController.generateAiScript` 保持一致。

---

### 1.9 广播剧分角色配音 axios 超时（10s 默认超时导致中断）

**问题**：广播剧 32 句台词分角色配音时，合成到 20 多句突然报 `timeout of 10000ms exceeded`，前面已合成的音频全部丢失。

**根因**：
1. `request.js` 全局默认超时 10s
2. `StudioWorkbench.vue` 中逐句调用 `/tts/v2/synthesize` 未设置独立超时
3. 情感标签（如"冷漠傲慢""愤怒爆发"）会使 TTS 处理时间增加，容易超过 10s

**修复（已实施 ✅）**：
```javascript
// StudioWorkbench.vue — TTS v2 合成
request.post('/tts/v2/synthesize', { ... }, { timeout: 60000 })

// studio.js — 剧本解析
parseScript: (sectionId) => request.post('/studio/parse-script', { sectionId }, { timeout: 60000 }),

// studio.js — 音频拼接
concatAudio: (audioUrls) => request.post('/studio/concat-audio', { audioUrls }, { timeout: 120000 }),
```

---

### 1.1 支付回调并发竞态（CAS 原子更新）

**问题**：Alipay notify 重复推送时，旧代码"先查再改"会产生并发窗口，导致 VIP 重复激活或激活失败。

**根因**：
```java
// ❌ 旧代码：check-then-act，非原子
VipOrder order = mapper.selectOne(WHERE orderNo = ?);
if ("pending".equals(order.getStatus())) {
    order.setStatus("paid");
    mapper.updateById(order); // 并发时两个线程都能走到这里
}
```

**修复（已实施 ✅）**：
```java
// ✅ CAS 原子更新：MySQL 行锁保证串行
int updated = mapper.update(null,
    new LambdaUpdateWrapper<>()
        .eq(VipOrder::getOrderNo, orderNo)
        .eq(VipOrder::getStatus, "pending")   // WHERE status='pending'
        .set(VipOrder::getStatus, "paid"));

if (updated == 0) return "success";  // 幂等跳过
activateUserVip(paid);               // 只有 updated=1 才激活
```

**企业级最佳实践**：
- DB CAS（当前实现）：MySQL 行锁，适合单库场景
- Redis 分布式锁（Redisson）：适合多库/微服务场景
- MQ 幂等消费（事务消息）：RocketMQ 事务消息，彻底解耦

---

### 1.2 @Qualifier 与 @RequiredArgsConstructor 冲突

**问题**：`PodcastWebSocketHandler` 使用 Lombok `@RequiredArgsConstructor` + final 字段 `@Qualifier("r2UploadExecutor")`，Lombok 生成构造函数时不会把字段上的 `@Qualifier` 传递到构造参数，Spring 无法决策注入哪个 `Executor` Bean → `NoUniqueBeanDefinitionException` → 应用启动失败 → WebSocket 端点未注册 → 播客生成无响应。

**修复（已实施 ✅）**：
```java
// ✅ 改为非 final 字段 + @Autowired + @Qualifier（Spring 字段注入正确处理）
@Autowired
@Qualifier("r2UploadExecutor")
private Executor r2UploadExecutor;
```

**原则**：使用 `@RequiredArgsConstructor` 时，若需要 `@Qualifier` 注入，必须改用字段注入或手动构造函数。

---

### 1.3 Agent 工坊 MiniMax Tool Calling 标记泄漏

**问题**：`AgentController` 的 `TOOL_CALLING_PROVIDERS` 包含 minimax，但 MiniMax 使用自有 Tool Calling 格式（非 OpenAI 标准），LangChain4j 无法解析，原始标记透出到 C 端聊天框：
```
——function< | tool_sep | >generateScript:"温馨","theme":"动物冒险"...
< | tool_call_end | >< | tool_calls_end | >
```

**修复（已实施 ✅）**：
1. 从 `TOOL_CALLING_PROVIDERS` 移除 `minimax`
2. 新增 `cleanReply()` 方法过滤四类模型内部标记（DeepSeek `<think>`、MiniMax 宽松/紧凑格式、`[TOOL_CALLS]`）

---

### 1.4 BOM 字符导致编译失败

**问题**：PowerShell `Set-Content -Encoding UTF8` 会自动在文件开头加 UTF-8 BOM（`\xEF\xBB\xBF`），Java 编译器报：`非法字符: '\ufeff'`。

**修复（已实施 ✅）**：用 `write_to_file` 工具（无 BOM UTF-8）重写受影响文件。

**原则**：Windows 环境下写 Java 文件，必须用 `UTF-8 without BOM`。

---

### 1.5 WebSocket 路径双 /ws 导致 404

**问题**：
- Cloudflare 环境变量：`VITE_WS_BASE_URL = wss://joyoai.xyz/ws`（含 `/ws`）
- 前端 endpoint：`/ws/podcast`（也含 `/ws`）
- 组合结果：`wss://joyoai.xyz/ws/ws/podcast`（双层 `/ws`）
- Nginx → Spring Boot 收到 `/ws/ws/podcast`，无 WebSocket 注册 → `NoResourceFoundException`

**修复（已实施 ✅）**：endpoint 改为 `/podcast`，与 base URL 正确拼接：
```
wss://joyoai.xyz/ws + /podcast = wss://joyoai.xyz/ws/podcast ✓
```

---

### 1.6 注册改为邀请码模式（移除 SMS）

**问题**：原验证码注册依赖短信服务，成本高且测试环境写死验证码（安全风险）。

**修复（已实施 ✅）**：
- 新增 `invite_code` 表（V14 迁移），预置初始邀请码
- 注册流程：手机号 + 邀请码 + 密码，后端校验邀请码有效性 + 使用次数 CAS 递增
- 新增 Admin 接口：`POST /api/auth/admin/invite-code`（生成码）、`GET /api/auth/admin/invite-codes`（查询）

---

## 二、策略缓存一致性问题（核心分析）

### 2.1 问题描述

运营端（本地 Spring Boot）修改 `sys_tier_policy` 后，C 端用户未能实时感知，出现以下现象：
- 修改功能开关后，用户依然被拦截
- 修改配额后，用户依然受旧配额限制

### 2.2 根因分析（三层缓存）

```
运营端修改 DB
     │
     ▼
TierPolicyController.update() → refreshAll()
     │
     ├─→ 清本地运营端 JVM L1（ConcurrentHashMap）✓
     ├─→ 写 Redis（sys:policy:*）✓
     │
     × 生产服务器 JVM L1 未通知 → getByTierCode() 命中旧 L1
     × getByTierCode() 只走：L1 → DB，根本不读 Redis！（Redis 写了没人读）
     × 前端 authStore.policy 登录时下发，从不刷新
```

### 2.3 修复方案对比

#### 方案 A：纯 TTL（最简单）
```java
// 用 Caffeine 替换 ConcurrentHashMap，设置 5 分钟 TTL 自动失效
LoadingCache<String, SysTierPolicy> localCache = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(tierCode -> fetchFromDb(tierCode));
```
- **优点**：零代码侵入，简单可靠
- **缺点**：最长 5 分钟延迟，不能做到秒级
- **适用**：配置变更不频繁、可接受分钟级延迟的场景

#### 方案 B：Redis Pub/Sub 广播（当前实施 ✅）
```
运营端 refreshAll()
  → 写 Redis（最新数据）
  → publish("soundread:policy:refresh", timestamp)
       │
       ▼（所有订阅节点）
生产服务器 收到消息 → localCache.clear()
  → 下次 getByTierCode() L1 Miss
  → 读 Redis（最新）→ 回填 L1 ✓
```
- **优点**：秒级生效，多节点广播
- **缺点**：消息可能丢失（Redis 宕机），需要 getByTierCode 读 Redis 兜底

#### 方案 C：Apollo/Nacos 配置中心（企业级）
```
运营端修改 Apollo Portal
  → Apollo Server 持久化配置
  → 长连接推送给所有订阅节点（Spring @RefreshScope）
  → 节点自动刷新 Bean，毫秒级生效
  → 支持版本回滚、灰度发布
```
- **优点**：毫秒级，支持灰度、回滚、审计
- **缺点**：引入额外中间件，运维成本高
- **适用**：超过 3 个节点的微服务部署

### 2.4 已实施方案（已提交 ✅）

**后端**：
1. `TierPolicyService.getByTierCode()` 新增 Redis 读取层（L1 Miss → Redis → DB）
2. `TierPolicyService.refreshAll()` 发布 Pub/Sub 消息至 `soundread:policy:refresh`
3. 所有节点订阅频道，收到消息后清除 L1
4. 新增 `RedisConfig.java` 注册 `RedisMessageListenerContainer` Bean

**前端**（Stale-While-Revalidate）：
```javascript
// router/index.js 路由守卫
if (authStore.token && !authStore.user) {
    await authStore.fetchUserInfo()  // 首次：阻塞等待
} else if (authStore.token && authStore.user) {
    authStore.fetchUserInfo().catch(() => {})  // 已登录：后台静默刷新
}
```
- 用户看到的始终是当前页面（不阻塞导航）
- 下次导航时 policy 已更新

---

## 三、待优化方案（Future Roadmap）

### 3.1 ⏳ 支付对账任务（Solution 2）

**背景**：Alipay notify 重试最多 25 次/25 小时。若服务器宕机超时，notify 全部失败，出现"已付款未激活 VIP"的孤立状态。

**方案**：定时对账任务，每小时主动向 Alipay 拉取 `pending` 订单状态。

```java
@Scheduled(cron = "0 0 * * * ?")
public void reconcile() {
    // 查找超过 10 分钟仍为 pending 的订单
    List<VipOrder> orders = vipOrderMapper.selectPendingTimeout(10);
    for (VipOrder order : orders) {
        AlipayTradeQueryResponse resp = alipayClient.execute(queryReq);
        if ("TRADE_SUCCESS".equals(resp.getTradeStatus())) {
            vipService.forceActivate(order);  // 补激活
        }
    }
}
```

**优先级**：高（涉及资金安全）

---

### 3.2 ⏳ 管理员 VIP 手动激活接口

**背景**：对账任务兜底后，仍存在极端情况（Alipay 接口异常、网络隔离）导致无法自动激活。

**方案**：运营后台提供手动激活入口：
```
POST /api/admin/vip/activate
Body: { "userId": 123, "planCode": "month", "remark": "手动补偿" }
```

**优先级**：中

---

### 3.3 ⏳ RocketMQ 异步支付处理（Solution 3）

**背景**：当前 Alipay notify 同步处理（激活 VIP + 写 DB），如果 DB 慢会导致 notify 超时，Alipay 误判失败。

**方案**：
```
Alipay notify → 验签 → 发 MQ Topic（soundread:payment:notify）
→ 立即返回 "success"（Alipay 不等待）
→ Consumer 异步处理激活 VIP
→ MQ 自动重试（失败最多重试 N 次，间隔递增）
```

**优先级**：中（RocketMQ 已接入，可直接使用）

---

### 3.4 ⏳ 用户 tierCode 变更后缓存清理

**背景**：`AuthService.evictUserCache()` 在 VIP 激活时调用，但管理员手动改 `tier_code`（直接 DB 或通过 Admin API）后，用户缓存未清理，最长 60 秒后才生效。

**方案**：Admin 更新用户 tierCode 的接口中调用 `evictUserCache(userId)`。

---

### 3.5 ⏳ 前端策略变更实时推送（SSE）

**背景**：当前用路由守卫"每次导航后台拉取"，用户需要切换页面后才感知策略变更。

**方案**：服务端 SSE（Server-Sent Events）推送策略变更通知：
```java
// 后端：运营修改后，往 SSE 推送
@GetMapping("/policy/events")
public SseEmitter policyEvents() { ... }

// refreshAll() 末尾广播
policyEventEmitter.send("policy_updated");
```
```javascript
// 前端：自动拉取最新 policy
const evtSource = new EventSource('/api/policy/events')
evtSource.onmessage = () => authStore.fetchUserInfo()
```

**优先级**：低（当前路由守卫方案已够用）

---

### 3.6 ⏳ 监控告警体系

**背景**：目前缺乏关键指标监控，问题只能被动发现。

**方案**：Prometheus + Grafana 监控面板：

| 指标 | 告警阈值 |
|------|---------|
| Alipay notify 失败率 | > 5% 告警 |
| VIP 激活失败次数 | > 0 告警 |
| pending 订单 > 10 分钟 | > 0 告警 |
| Redis 缓存 miss 率 | > 30% 告警 |
| API P99 响应时间 | > 2s 告警 |

**优先级**：中

---

## 四、企业级方案总结

### 支付可靠性

```
三道防线（当前）:
  签名验证 → DB CAS → @Transactional 回滚

企业级（规划中）:
  + 对账定时任务（T+1 补偿）
  + MQ 异步处理（解耦、可重试）
  + 监控告警（实时感知）
```

### 缓存一致性

```
两级缓存（当前）:
  L1 JVM（Pub/Sub 清除）→ L2 Redis → DB

企业级（可选）:
  L1 Caffeine TTL → L2 Redis → 配置中心（Apollo/Nacos）
  + SSE 推送给前端（毫秒级）
```

### 注册安全

```
当前:
  邀请码（一次性/有上限/可过期）

企业级可扩展:
  + 邀请码与渠道来源绑定（追踪转化）
  + 邀请码发放数量自动统计
  + 手机号实名验证（出于合规）
```
