# SoundRead 权限鉴定架构解析 (基于 Sa-Token)

本项目采用了 **Sa-Token** 作为全栈认证授权框架。权限控制体系分为三层防御，主要通过**全局路由拦截**、**Session 标识校验** 和 **AOP 业务权限切面** 来实现多维度的保护。

---

## 🛡️ 第一层防御：全局路由拦截（网关级控制）

**核心文件**：`server/src/main/java/com/soundread/config/SaTokenConfig.java`

这是请求到达任何后端业务代码前的第一道大门。它负责基础的“登录校验”和模块级别的安全隔离。

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new SaInterceptor(handle -> {
        // 1. 跳过 CORS 预检请求（OPTIONS），避免跨域问题
        if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
            return;
        }

        // 2. 【普通用户必须登录的黑名单】
        // 只要访问这些 /api 开头的模块，系统就会自动调用 StpUtil.checkLogin();
        // 如果未带 token 或 token 已过期失效，会直接抛出 401 异常打回前端。
        SaRouter.match("/api/tts/**", "/api/podcast/**", "/api/voice/**",
                        "/api/vip/**", "/api/studio/**", "/api/creation/**",
                        "/api/novel/**", "/api/music/**", "/api/discover/works/*/like")
                .check(r -> StpUtil.checkLogin());

        // 3. 【管理员专属保护】（详见第二层防御）
        SaRouter.match("/api/admin/**", r -> { ... });

    // 4. 【拦截所有 /api/**】
    })).addPathPatterns("/api/**") 
       
       // 5. 【绝对白名单区：完全不拦截，游客可用】
       .excludePathPatterns( 
            "/api/auth/**",                   // 登录注册、验证码
            "/api/discover/banners",          // 发现页轮播图
            "/api/discover/works",            // 发现页公共作品列表
            "/api/tts/voices",                // 获取通用音色列表
            "/api/vip/plans",                 // 会员套餐价格查询
            "/api/vip/payment/alipay-notify", // 支付宝支付异步回调（绝对不能拦截）
            "/api/studio/templates"           // 创作中心可用的模版列表
       );
}
```

### 💡 哪些接口是不用判断权限的？
在 `excludePathPatterns` 清单里的接口都不需要验证权限（即**前端请求不用带 Authorization Token**）。这些通常是：
- 用户身份无关的公共数据（首页展示、价目表）。
- 登录、注册此类本身需要获取 Token 的操作。
- 第三方服务器的回调（如支付网关）。

---

## 🛡️ 第二层防御：管理员鉴权（角色/属性控制）

有些接口（例如控制面板数据统计、强制刷新套餐缓存 `/api/admin/policy/refresh`）只能由**运营后台**或**超级管理员**访问。

在 `SaTokenConfig.java` 中，针对 `/api/admin/**` 的匹配独立设计了检查：

```java
SaRouter.match("/api/admin/**", r -> {
    // 1. 必须先登录
    StpUtil.checkLogin(); 
    
    // 2. 从 Session 中提取关键标识
    Object isAdmin = StpUtil.getSession().get("admin");
    if (!Boolean.TRUE.equals(isAdmin)) {
        throw new com.soundread.common.exception.BusinessException(403, "缺少运营管理访问权限");
    }
});
```

### 💡 这里的 Session 标识来自哪里？
在用户登录时（`AuthService.java` 的 `doLogin` 方法），如果我们查表发现这是一个管理员账号，我们会主动给当前用户的 Session 贴上一个标签：
```java
// 登录成功发 token 后，挂载管理员标记
StpUtil.getSession().set("admin", true);
```
之后哪怕他试图调用后台的接口，这个动态拦截器也会去拿这个标记判断，没有就抛 403 拒绝访问。

---

## 🛡️ 第三层防御：AOP 业务权限切面（细粒度限制）

除了基础的登录与角色隔离，项目存在复杂的**业务套餐限制**。
例如：“大家都登录了，但普通用户不能生成广播剧，只有购买了高级套餐的用户才能用”。

**核心文件**：`server/src/main/java/com/soundread/common/FeatureCheckAspect.java` 和 `@RequireFeature` 注解。

```java
// 使用示例（修改前的广播剧接口）：
@RequireFeature("ai_drama")
@PostMapping("/projects/{id}/drama-generate")
public SseEmitter generateDrama(...) { ... }
```

### 运行机制：
1. `FeatureCheckAspect` 扫描到该请求头上带有 `@RequireFeature`。
2. 切面去获取当前用户的 `tierCode` (如 `free`, `vip`)。
3. 从缓存/数据库查询该套餐是否开启了 `ai_drama` 这个功能。
4. 如果没开，切面直接拦截并在方法执行**前**抛出 403 异常。

### ⚠️ 关于 AOP 和 SSE 流式响应的冲突警示：
在使用 **SSE (Server-Sent Events) 单向长连接** 接口（如广播剧流式生成）时，**不适用**这种 AOP 抛异常拦截！

因为 AOP 抛出的异常会被 Spring 的全局异常处理器捕获为 JSON（`{"code": 403, "msg": "无权限"}`）。但 SSE 请求要求返回 `text/event-stream` 内容类型。两者发生**内容协商冲突**，导致 Spring Boot 直接返回空内容的 `405 Method Not Allowed`，这就导致了前端看不见真正的报错信息。

因此，对于流式生成的接口，正确的做法是**去掉注解，改用“内联判断”（Inline Check）**，也就是我们近期修复广播剧 0 字符的办法：

```java
// 修复后的做法：内联判定 + 写入流错误事件
if (!tierPolicyService.hasFeature(user.getTierCode(), "ai_drama")) {
    // 主动给前端流发一个错误标识，让前端在JS里抛大弹窗
    emitter.send(SseEmitter.event().data("[DRAMA_ERROR]当前套餐未开通广播剧功能，请升级会员"));
    emitter.complete();
    return emitter;
}
```

---

## 总结最佳使用姿势

1. **不需要任何权限的公共接口 (如浏览数据)**：放进 `SaTokenConfig` 的 `excludePathPatterns`。
2. **需要防爬虫/关联用户的接口**：利用统一的 `/api/...` `checkLogin()` 拦截（普通拦截）。
3. **敏感操作/运维后台专属**：放到 `/api/admin/...` 控制器下统一拦截，校验 session label。
4. **功能等级收费拦截 (如AI小说/播客/音乐)**：
   - 如果是**普通 REST 接口**：在控制器方法上面挂 `@RequireFeature("功能代号")`。
   - 如果是**SSE 生成接口**：在代码内部手动写 `tierPolicyService.hasFeature(...)` 检查！
