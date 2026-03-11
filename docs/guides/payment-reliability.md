# 支付系统可靠性优化方案

> 版本：v1.0 | 2026-03-11  
> 项目：SoundRead（支付宝手机网站支付 + 电脑网站支付）

---

## 一、现状分析与风险清单

### 1.1 当前支付链路

```
用户点击"开通 VIP"
    ↓
后端创建订单 (vip_order, status='pending')
    ↓
调用支付宝 alipay.trade.page.pay / wap.pay
    ↓
用户跳转支付宝完成支付
    ↓
支付宝异步 POST /api/vip/payment/alipay-notify（可重试 25 次）
    ↓
验签 → 更新 status='paid' → 激活用户 VIP
    ↓
前端轮询 /api/vip/order/{orderNo}/status 感知结果
```

### 1.2 风险清单（已识别）

| 编号 | 风险点 | 级别 | 状态 |
|------|--------|------|------|
| R1 | **并发 notify 竞态** — 多个 notify 同时到达，`if(status==pending)` 无法防并发，导致双写激活 | 🔴 严重 | ✅ 已修复（CAS） |
| R2 | **activateVip 失败orphan** — 旧代码先改 status='paid' 再激活，若激活失败下次 notify 幂等跳过，VIP 永不激活 | 🔴 严重 | ✅ 已修复（事务回滚） |
| R3 | **notify 接收失败** — 服务器宕机期间支付宝推送，重试 25 次（25小时）后放弃 | 🟡 中等 | ⏳ 待做（对账任务） |
| R4 | **用户付款但未开通** — 前端没有感知（依赖轮询，未做超时提示） | 🟡 中等 | ⏳ 待做（文档方案2） |
| R5 | **重复下单** — 同一用户在第一笔未支付时可继续下单，可能产生多笔待支付订单 | 🟢 低 | ⏳ 待做（文档方案3） |

---

## 二、已实施：方案一 — DB CAS 原子更新

### 2.1 问题

旧代码：**先查状态，再修改** — 两步操作之间有窗口期，并发 notify 都能通过检查

```java
// ❌ 旧代码（存在竞态）
if ("paid".equals(order.getStatus())) { return "success"; }  // 检查
vipOrderMapper.updateById(order.setStatus("paid"));           // 修改（两步之间有窗口）
activateUserVip(order);  // 若这里异常，status已改为paid，下次notify幂等跳过→永不激活
```

### 2.2 修复

```java
// ✅ 新代码（CAS 原子操作）
int updated = vipOrderMapper.update(null,
    new LambdaUpdateWrapper<VipOrder>()
        .eq(VipOrder::getOrderNo, orderNo)
        .eq(VipOrder::getStatus, "pending")   // ← CAS 条件
        .set(VipOrder::getStatus, "paid")
        ...);

if (updated == 0) { return "success"; }  // 已被处理，幂等跳过

// @Transactional 保证：若 activateUserVip 抛异常
// → 事务回滚 → status 回到 pending → 支付宝重试时可再次激活
activateUserVip(paid);
```

### 2.3 并发场景图

```
notify1: UPDATE WHERE status='pending' → 成功(updated=1) → 激活VIP
notify2: UPDATE WHERE status='pending' → 失败(updated=0) → 幂等返回success
             ↑ MySQL 行锁，天然串行，无竞态
```

---

## 三、待实施：方案二 — 对账任务（R3/R4 兜底）

> 适用场景：服务器短暂宕机，25小时内重试全部失败；用户付款后长时间未开通

### 3.1 对账原理

支付宝提供 `alipay.trade.query` 接口，主动查询订单真实支付状态。对账任务每小时扫描"超过10分钟仍为pending"的订单，主动向支付宝确认，若已支付则补激活。

### 3.2 实现方案

```java
// ReconcileJob.java（每小时执行）
@Scheduled(cron = "0 0 * * * ?")
public void reconcileOrders() {
    // 找所有创建超过10分钟仍为 pending 的订单
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
    List<VipOrder> pendingOrders = vipOrderMapper.selectList(
        new LambdaQueryWrapper<VipOrder>()
            .eq(VipOrder::getStatus, "pending")
            .lt(VipOrder::getCreatedAt, threshold));

    for (VipOrder order : pendingOrders) {
        try {
            // 主动查询支付宝
            AlipayTradeQueryRequest req = new AlipayTradeQueryRequest();
            req.setBizContent("{\"out_trade_no\":\"" + order.getOrderNo() + "\"}");
            AlipayTradeQueryResponse resp = alipayClient.execute(req);
            
            if (resp.isSuccess() && "TRADE_SUCCESS".equals(resp.getTradeStatus())) {
                // 支付宝显示已支付，但我们未收到 notify → 补激活
                vipService.forceActivate(order, resp.getTradeNo());
                log.info("[Reconcile] 补激活: orderNo={}", order.getOrderNo());
            } else if ("TRADE_CLOSED".equals(resp.getTradeStatus())) {
                // 订单已关闭（超时未付）→ 标记关闭
                vipOrderMapper.updateById(order.setStatus("expired"));
            }
        } catch (Exception e) {
            log.error("[Reconcile] 查询失败: orderNo={}", order.getOrderNo(), e);
        }
    }
}
```

---

## 四、待实施：方案三 — RocketMQ 异步解耦（企业级方案）

> 项目已接入 RocketMQ，扩展成本低。适合：高并发、多实例、99.99% 可靠性要求

### 4.1 架构

```
支付宝 notify
    ↓
Controller 接收 → 验签 → 发 MQ Topic("vip-payment-success") → 立即返回 "success"
                                    ↓
                           Consumer 消费（异步）
                               ↓            ↓
                           DB操作          缓存清除
                           失败 → MQ 自动重试 16 次（间隔 1s→5s→... 最长30天）
```

### 4.2 优势

| 特性 | 同步方案（现方案） | MQ 异步方案 |
|------|-----------------|------------|
| 可用性 | DB 宕机 → notify 失败 → 支付宝重试 | DB 宕机 → 消息存 MQ → DB 恢复后自动处理 |
| 并发 | 行锁解决 | 单线程 Consumer 天然串行 |
| 重试 | 依赖支付宝（25次/25h） | MQ 自管（16次/30天） |
| 扩展 | 激活逻辑串行 | 可 fanout 到多个 Consumer（短信通知、积分赠送等） |

### 4.3 实现要点

```java
// Producer（在 notify 接收完验签后）
rocketMQTemplate.asyncSend("vip-payment-success",
    MessageBuilder.withPayload(orderNo).build(), 
    new SendCallback() { ... });

// Consumer
@RocketMQMessageListener(topic = "vip-payment-success", consumerGroup = "vip-consumer")
public class VipPaymentConsumer implements RocketMQListener<String> {
    @Override
    public void onMessage(String orderNo) {
        vipService.processPayment(orderNo);  // 幂等处理
    }
}
```

---

## 五、待实施：方案四 — Redis 分布式锁（多实例扩展时）

> 当服务扩展到多节点时，CAS 数据库行锁依然有效，Redis 锁可作为额外防护层

```java
String lockKey = "lock:vip:notify:" + orderNo;
Boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "1", Duration.ofSeconds(30));  // NX + EX 原子操作

if (!Boolean.TRUE.equals(acquired)) {
    return "success";  // 其他实例正在处理
}
try {
    // 处理逻辑...
} finally {
    redisTemplate.delete(lockKey);
}
```

---

## 六、企业级支付系统标准架构

```
┌─────────────────────────────────────────────────────────┐
│                    企业级支付可靠性                        │
├─────────────────────────────────────────────────────────┤
│  1. 防伪造    │ RSA2 验签（必做）                          │
│  2. 防重放    │ nonce + timestamp 校验（防 5 分钟内重放）   │
│  3. 防并发    │ DB CAS / Redis 分布式锁                    │
│  4. 事务保证  │ @Transactional（激活失败自动回滚）          │
│  5. 异步解耦  │ MQ（支付与业务解耦，失败独立重试）           │
│  6. 对账兜底  │ 定时任务主动拉取支付宝数据（T+1 对账）       │
│  7. 监控告警  │ 异常 notify 率、未激活率监控               │
│  8. 人工干预  │ 管理后台"手动激活"入口                     │
└─────────────────────────────────────────────────────────┘
```

---

## 七、实施进度

| 方案 | 内容 | 状态 |
|------|------|------|
| 方案一 | DB CAS 原子更新防竞态 | ✅ 已实施（commit dacf165 之后） |
| 方案二 | 对账定时任务 | ⏳ 待实施 |
| 方案三 | RocketMQ 异步解耦 | 📋 规划中 |
| 方案四 | Redis 分布式锁（多实例） | 📋 规划中（单机暂不需要） |
