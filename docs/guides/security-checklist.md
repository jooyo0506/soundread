# SoundRead 安全检查清单

> 每次功能上线前、每次 Code Review 时必须核对此清单。
> 标记 🔴 为高危，必须修复；🟡 为中危，强烈建议修复；🟢 为低危/建议项。

---

## 一、身份认证与授权

### 1.1 接口访问控制

- [ ] 🔴 所有非公开接口是否配置了 Sa-Token 拦截（检查 `SaTokenConfig.java` 排除白名单）
- [ ] 🔴 白名单路径是否精确到最小范围（不能用 `/api/**` 放行所有）
- [ ] 🔴 用户只能操作自己的数据（Service 层必须校验 `userId` 匹配）
- [ ] 🟡 管理员接口是否有独立的角色权限校验（Admin Controller 应检查用户角色）
- [ ] 🟡 Token 失效后是否正确返回 401（不能返回 200 + 空数据）

**检查代码示例：**

```java
// ✅ 每个涉及数据修改的 Service 方法必须校验归属
public void deleteProject(Long projectId) {
    StudioProject project = projectMapper.selectById(projectId);
    Long currentUserId = StpUtil.getLoginIdAsLong();
    if (project == null || !project.getUserId().equals(currentUserId)) {
        throw new BusinessException("无权操作该项目");  // ✅
    }
}

// ❌ 危险：未校验归属，任何登录用户都能删除他人数据
public void deleteProject(Long projectId) {
    projectMapper.deleteById(projectId);  // ❌ SQL 注入风险 + 越权访问
}
```

### 1.2 Token 安全

- [ ] 🔴 JWT Secret 是否使用强随机密钥（至少 32 位随机字符串）
- [ ] 🔴 JWT Secret 是否通过环境变量注入（不能硬编码在配置文件中）
- [ ] 🟡 Token 有效期设置是否合理（当前 30 天，注意账号泄露风险）
- [ ] 🟡 敏感操作（修改密码/绑定手机）是否要求重新验证

---

## 二、输入验证与 SQL 安全

### 2.1 用户输入处理

- [ ] 🔴 所有接受用户输入的文本字段是否有长度限制（防止超大请求攻击）
- [ ] 🔴 文件上传（如音频、封面）是否限制了文件类型和大小
- [ ] 🟡 TTS 合成接口输入文本是否限制了最大长度
- [ ] 🟡 AI 相关接口的 Prompt 参数是否有注入风险检查

**关键输入校验清单：**

| 接口 | 关键参数 | 限制要求 |
|------|---------|---------|
| TTS 合成 | `text` | 最大长度 5000 字 |
| 有声书创建 | `rawText` | 最大长度 500000 字 |
| Studio 内容生成 | `userInput` | 最大长度 2000 字 |
| 用户注册 | `phone`/`email` | 格式正则校验 |
| 音色购买 | `voiceId` | 必须在 sys_voice 白名单内 |

### 2.2 SQL 安全

- [ ] 🔴 全部 SQL 操作是否使用 MyBatis-Plus 参数绑定（无拼接 SQL）
- [ ] 🔴 动态排序字段是否白名单校验（防止 SQL 注入）
- [ ] 🟡 分页参数是否有合法范围校验（page ≥ 1，size ≤ 100）

```java
// ✅ 安全：MyBatis-Plus LambdaWrapper 参数绑定
mapper.selectList(new LambdaQueryWrapper<Work>()
    .eq(Work::getUserId, userId)
    .orderByDesc(Work::getCreatedAt));

// ❌ 危险：字符串拼接，存在 SQL 注入风险
String sql = "SELECT * FROM work WHERE user_id = " + userId;

// ✅ 分页参数校验
if (page < 1) page = 1;
if (size > 100) size = 20;   // 限制最大 size
```

---

## 三、敏感数据与密钥管理

### 3.1 密钥和配置安全

- [ ] 🔴 `application.yml`（真实密钥版）是否已在 `.gitignore` 中排除
- [ ] 🔴 所有第三方 API Key 是否通过环境变量注入（`${ENV_VAR:default}`）
- [ ] 🔴 数据库密码、Redis 密码、JWT Secret 是否均通过环境变量配置
- [ ] 🔴 Git 历史中是否曾经提交过真实密钥（需扫描 git log）

**必须通过环境变量配置的 Key 清单：**

```bash
DB_PASSWORD                    # MySQL 密码
REDIS_PASSWORD                 # Redis 密码
JWT_SECRET                     # JWT 签名密钥（≥32位随机字符串）
DOUBAO_API_KEY                 # 豆包 LLM API Key
VOLCENGINE_APP_ID              # 火山引擎 App ID
VOLCENGINE_ACCESS_TOKEN        # 火山引擎 Access Token
R2_ACCESS_KEY_ID               # Cloudflare R2 访问密钥
R2_SECRET_ACCESS_KEY           # Cloudflare R2 私钥
DEEPSEEK_API_KEY               # DeepSeek API Key（如启用）
MINIMAX_API_KEY                # MiniMax API Key（如启用）
```

### 3.2 日志脱敏

- [ ] 🔴 日志中是否不打印密码、Token、API Key
- [ ] 🟡 日志中是否不打印用户手机号、身份证等 PII 信息（至少部分脱敏）
- [ ] 🟡 SSE 推送的错误消息是否不包含系统内部堆栈信息

```java
// ✅ 脱敏示例
log.info("用户登录: phone={}***{}", phone.substring(0,3), phone.substring(7));

// ❌ 禁止
log.info("用户登录: phone={} password={}", phone, password);

// ✅ 异常日志：完整堆栈给开发者，简短消息给前端
log.error("[Auth] 登录失败: userId={}", userId, e);            // 服务器日志
emitter.send(SseEmitter.event().data("[AI 生成失败，请重试]")); // 前端消息
```

---

## 四、业务安全

### 4.1 配额与限速

- [ ] 🟡 TTS 合成接口是否有每日配额限制（防止恶意刷接口）
- [ ] 🟡 AI 生成接口是否有频率限制（防止大量并发请求烧 LLM 费用）
- [ ] 🟡 文件上传是否有存储配额限制（`StorageQuotaService` 校验）
- [ ] 🟢 是否有接口级别的 IP 限速

**配额检查代码：**

```java
// ✅ 合成前必须检查配额
quotaService.checkAndConsume(userId, text.length());

// ✅ 存储上传前必须检查配额
storageQuotaService.checkQuota(userId, fileSize);
```

### 4.2 支付安全

- [ ] 🔴 VIP 订单是否在服务端校验金额（不能信任前端传来的金额）
- [ ] 🔴 音色购买是否使用幂等性控制（重复支付回调不能重复发货）
- [ ] 🔴 支付回调接口是否有签名验证
- [ ] 🟡 订单状态是否只允许单向流转（不能从已完成变回待支付）

### 4.3 文件存取安全

- [ ] 🔴 R2 存储的文件 URL 是否做了访问控制（私有文件不能被外部直接枚举）
- [ ] 🟡 用户能否通过构造 URL 访问他人的私有音频文件
- [ ] 🟡 文件上传是否只允许指定 MIME 类型（`audio/*`、`image/*`）

---

## 五、跨域与请求安全

### 5.1 CORS 配置

- [ ] 🔴 CORS 允许域名是否精确配置（生产环境禁止 `allowedOrigins("*")`）
- [ ] 🟡 开发环境的 CORS 配置是否与生产隔离

```java
// ✅ 生产环境配置精确域名
.allowedOrigins("https://soundread.com", "https://www.soundread.com")

// ❌ 禁止生产使用通配符
.allowedOrigins("*")
```

### 5.2 请求头安全

- [ ] 🟡 是否配置了必要的安全响应头：
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Content-Security-Policy`（如需要）
- [ ] 🟢 是否禁止了不必要的 HTTP 方法（如 TRACE）

---

## 六、WebSocket 安全

- [ ] 🔴 WebSocket 握手是否验证了 Token（`PodcastWebSocketHandler`、`InteractionWebSocketHandler`）
- [ ] 🔴 WebSocket 接收的消息内容是否做了大小限制
- [ ] 🟡 WebSocket 连接是否在用户登出时主动断开
- [ ] 🟢 单个用户的 WebSocket 并发连接数是否有限制

```java
// ✅ WebSocket 建立连接时校验身份
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    String token = extractToken(session);
    if (!StpUtil.isLogin(token)) {
        session.close(CloseStatus.NOT_ACCEPTABLE);
        return;
    }
}
```

---

## 七、依赖安全

- [ ] 🟡 定期检查 Maven/npm 依赖是否有高危 CVE

```bash
# Maven 依赖安全扫描
cd server && mvn dependency:check

# npm 依赖安全扫描
cd web && npm audit

# 修复可自动修复的漏洞
cd web && npm audit fix
```

- [ ] 🟡 第三方 SDK（TTS、Mureka）是否使用最新稳定版
- [ ] 🟢 是否有依赖版本锁定机制（`package-lock.json`, Maven `dependencyManagement`）

---

## 八、部署安全

- [ ] 🔴 生产服务器是否只开放必要端口（80/443，禁止 8080 直接公网暴露）
- [ ] 🔴 数据库是否只允许内网访问（禁止 MySQL/Redis 公网直连）
- [ ] 🔴 服务器 SSH 是否使用密钥认证（禁止密码登录）
- [ ] 🟡 是否使用 HTTPS（TLS 1.2+）全量加密
- [ ] 🟡 定期备份数据库（至少每天一次）
- [ ] 🟡 日志是否有轮转策略（防止磁盘打满）
- [ ] 🟢 是否配置了服务崩溃自动重启（systemd / Docker restart policy）

---

## 九、上线前完整检查流程（Checklist）

```
□ 1. git diff 检查有无密钥误提交
□ 2. 运行 mvn compile 确认编译通过
□ 3. 运行 npm audit 检查前端依赖漏洞
□ 4. 检查 application.yml 是否使用环境变量
□ 5. 过一遍接口白名单，确认非公开接口均受保护
□ 6. 检查新增接口是否有用户归属校验
□ 7. 检查新增接口是否有输入长度限制
□ 8. 检查日志中是否有敏感信息打印
□ 9. 确认 CORS 配置为精确域名
□ 10. 确认 WebSocket 有 Token 校验
```

---

## 十、已知历史问题（持续修复中）

| 问题描述 | 风险等级 | 状态 |
|---------|---------|------|
| 部分 AI 接口缺少频率限制 | 🟡 | 待实现 |
| WebSocket 单用户并发连接数未限制 | 🟡 | 待实现 |
| 安全响应头未全局配置 | 🟢 | 待实现 |
| 支付回调幂等性控制待完善 | 🔴 | 进行中 |

---

*Last updated: 2026-03-09*
