package com.soundread.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.soundread.common.exception.BusinessException;
import com.soundread.config.AlipayProperties;
import com.soundread.mapper.UserMapper;
import com.soundread.mapper.VipOrderMapper;
import com.soundread.model.dto.VipDto;
import com.soundread.model.entity.User;
import com.soundread.model.entity.VipOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * VIP 会员服务
 *
 * <h3>支付安全设计（高并发最佳实践）</h3>
 * <ol>
 * <li><b>RSA2 验签</b> — 防伪造回调</li>
 * <li><b>app_id + 金额校验</b> — 防跨应用伪造和金额篡改</li>
 * <li><b>三态 CAS 状态机</b> — pending→paid→activated，
 * CAS 后激活失败可通过支付宝重试自动恢复</li>
 * <li><b>SQL 原子激活 VIP</b> — GREATEST() 防并发读写覆盖</li>
 * <li><b>Redis 防重下单</b> — 同用户同套餐 5 秒内只能创建一个订单</li>
 * <li><b>超时关单</b> — 定时任务自动关闭 30 分钟未支付订单</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipService {

    private final VipOrderMapper vipOrderMapper;
    private final UserMapper userMapper;
    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProps;
    private final StringRedisTemplate redisTemplate;
    @Lazy
    private final AuthService authService;

    /** 订单创建防重 key 前缀 */
    private static final String ORDER_DEDUP_PREFIX = "order:create:";
    /** 防重窗口 5 秒 */
    private static final long ORDER_DEDUP_SECONDS = 5L;

    // ==================== 套餐配置 ====================

    private static final List<VipDto.PlanItem> PLANS = List.of(
            new VipDto.PlanItem("vip_month", "月度体验", new BigDecimal("30.00"), 30, null),
            new VipDto.PlanItem("vip_year", "年度 VIP", new BigDecimal("300.00"), 365, new BigDecimal("360.00")),
            new VipDto.PlanItem("vip_lifetime", "终身 VIP", new BigDecimal("1000.00"), 9999, null));

    public List<VipDto.PlanItem> getPlans() {
        return PLANS;
    }

    // ==================== 创建订单 + 获取支付宝跳转 URL ====================

    /**
     * 创建 VIP 订单
     *
     * <p>
     * 新增 Redis 防重：同一用户 + 同一套餐 5 秒内只能创建一个订单，
     * 防止用户快速双击产生大量垃圾 pending 订单。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public VipDto.OrderResponse createOrder(Long userId, VipDto.SubscribeRequest req, boolean isMobile) {
        VipDto.PlanItem plan = PLANS.stream()
                .filter(p -> p.getPlanId().equals(req.getPlanId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("无效的套餐: " + req.getPlanId()));

        // ★ Redis 防重（同用户同套餐 5 秒窗口）
        String dedupKey = ORDER_DEDUP_PREFIX + userId + ":" + plan.getPlanId();
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", ORDER_DEDUP_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(absent)) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        // 生成唯一订单号（时间戳 + 随机后缀）
        String orderNo = "SR" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

        VipOrder order = new VipOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPlanId(plan.getPlanId());
        order.setPlanName(plan.getName());
        order.setAmount(plan.getPrice());
        order.setDurationDays(plan.getDurationDays());
        order.setStatus("pending");
        vipOrderMapper.insert(order);

        // 调用支付宝接口（PC/WAP 自动适配）
        String payUrl = buildAlipayPayUrl(order, isMobile);

        VipDto.OrderResponse resp = new VipDto.OrderResponse();
        resp.setOrderNo(orderNo);
        resp.setPayUrl(payUrl);
        return resp;
    }

    /**
     * 构建支付宝支付跳转 URL
     */
    private String buildAlipayPayUrl(VipOrder order, boolean isMobile) {
        String bizContent = "{" +
                "\"out_trade_no\":\"" + order.getOrderNo() + "\"," +
                "\"total_amount\":\"" + order.getAmount() + "\"," +
                "\"subject\":\"声读 " + order.getPlanName() + "\"," +
                "\"product_code\":\"" + (isMobile ? "QUICK_WAP_WAY" : "FAST_INSTANT_TRADE_PAY") + "\"" +
                "}";
        try {
            if (isMobile) {
                AlipayTradeWapPayRequest req = new AlipayTradeWapPayRequest();
                req.setNotifyUrl(alipayProps.getNotifyUrl());
                req.setReturnUrl(alipayProps.getReturnUrl());
                req.setBizContent(bizContent);
                return alipayClient.pageExecute(req).getBody();
            } else {
                AlipayTradePagePayRequest req = new AlipayTradePagePayRequest();
                req.setNotifyUrl(alipayProps.getNotifyUrl());
                req.setReturnUrl(alipayProps.getReturnUrl());
                req.setBizContent(bizContent);
                return alipayClient.pageExecute(req).getBody();
            }
        } catch (AlipayApiException e) {
            log.error("支付宝支付请求失败: orderNo={}", order.getOrderNo(), e);
            throw new BusinessException("支付发起失败，请稍后重试");
        }
    }

    // ==================== 支付宝异步回调 ====================

    /**
     * 处理支付宝异步通知（编排方法，不加事务）
     *
     * <h3>六道可靠性防线</h3>
     * <ol>
     * <li>RSA2 验签 — 防伪造回调</li>
     * <li>app_id 校验 — 防跨应用伪造</li>
     * <li>金额校验 — 防支付金额篡改</li>
     * <li>三态 CAS — pending→paid 防并发双写</li>
     * <li>paid 状态重试恢复 — CAS 后激活失败时支付宝重试可恢复</li>
     * <li>SQL 原子激活 — 防 VIP 时间覆盖</li>
     * </ol>
     */
    public String handleAlipayNotify(Map<String, String> params) throws AlipayApiException {
        // ===== 1. 验签（防伪造） =====
        boolean signOk = AlipaySignature.rsaCheckV1(
                params, alipayProps.getPublicKey(), "UTF-8", "RSA2");
        if (!signOk) {
            log.warn("[VipNotify] 签名验证失败: params={}", params);
            return "fail";
        }

        // ===== 2. ★ app_id 校验（防跨应用伪造） =====
        String appId = params.get("app_id");
        if (!alipayProps.getAppId().equals(appId)) {
            log.warn("[VipNotify] app_id 不匹配: expected={}, actual={}", alipayProps.getAppId(), appId);
            return "fail";
        }

        // ===== 3. 只处理 TRADE_SUCCESS / TRADE_FINISHED =====
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success"; // 其他状态告知支付宝不再重试
        }

        String orderNo = params.get("out_trade_no");
        String alipayTradeNo = params.get("trade_no");

        // ===== 4. 查订单（确认存在）=====
        VipOrder order = vipOrderMapper.selectOne(
                new LambdaQueryWrapper<VipOrder>().eq(VipOrder::getOrderNo, orderNo));
        if (order == null) {
            log.error("[VipNotify] 订单不存在: orderNo={}", orderNo);
            return "fail";
        }

        // ===== 5. ★ 金额校验（防支付金额篡改） =====
        String totalAmount = params.get("total_amount");
        if (totalAmount == null || new BigDecimal(totalAmount).compareTo(order.getAmount()) != 0) {
            log.error("[VipNotify] 金额不匹配: orderNo={}, expected={}, actual={}",
                    orderNo, order.getAmount(), totalAmount);
            return "fail";
        }

        // ===== 6. ★ 三态 CAS 状态机 =====
        String currentStatus = order.getStatus();

        if ("activated".equals(currentStatus)) {
            // 已完成激活，幂等返回
            log.info("[VipNotify] 已激活，幂等跳过: orderNo={}", orderNo);
            return "success";
        }

        if ("pending".equals(currentStatus)) {
            // pending → paid（首次回调）
            int updated = vipOrderMapper.update(null,
                    new LambdaUpdateWrapper<VipOrder>()
                            .eq(VipOrder::getOrderNo, orderNo)
                            .eq(VipOrder::getStatus, "pending")
                            .set(VipOrder::getStatus, "paid")
                            .set(VipOrder::getAlipayTradeNo, alipayTradeNo)
                            .set(VipOrder::getPayTime, LocalDateTime.now())
                            .set(VipOrder::getNotifyRaw, params.toString()));
            if (updated == 0) {
                // 竞争失败，重新读状态走下方 paid 分支
                order = vipOrderMapper.selectOne(
                        new LambdaQueryWrapper<VipOrder>().eq(VipOrder::getOrderNo, orderNo));
                if (order == null || "activated".equals(order.getStatus())) {
                    return "success";
                }
            }
        }

        // ===== 7. paid 状态 → 激活 VIP → activated =====
        // 无论是首次 pending→paid 后，还是支付宝重试时 status 已经是 paid，都走这里
        activateUserVip(order);

        // paid → activated（标记激活完成）
        vipOrderMapper.update(null,
                new LambdaUpdateWrapper<VipOrder>()
                        .eq(VipOrder::getOrderNo, orderNo)
                        .eq(VipOrder::getStatus, "paid")
                        .set(VipOrder::getStatus, "activated"));

        log.info("[VipNotify] 处理成功: orderNo={}, alipayTradeNo={}", orderNo, alipayTradeNo);
        return "success";
    }

    /**
     * 原子激活用户 VIP
     *
     * <p>
     * 使用 SQL 原子操作避免并发读写竞争：
     * 到期时间从 MAX(当前到期时间, NOW()) 开始叠加天数，
     * VIP 等级取已有等级和新等级的较大值（不降级）。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void activateUserVip(VipOrder order) {
        int vipLevel = switch (order.getPlanId()) {
            case "vip_month" -> 1;
            case "vip_year" -> 2;
            case "vip_lifetime" -> 3;
            default -> 1;
        };

        String tierCode = switch (vipLevel) {
            case 2 -> "vip_year";
            case 3 -> "vip_lifetime";
            default -> "vip_month";
        };

        int durationDays = "vip_lifetime".equals(order.getPlanId()) ? 99999 : order.getDurationDays();

        // ★ 单条 SQL 原子完成，无读写竞争
        int rows = userMapper.atomicActivateVip(order.getUserId(), durationDays, vipLevel, tierCode);
        if (rows == 0) {
            throw new BusinessException("VIP 激活失败：用户不存在 userId=" + order.getUserId());
        }

        // 清除 Redis 用户缓存
        authService.evictUserCache(order.getUserId());

        log.info("VIP 激活: userId={}, tierCode={}, durationDays={}", order.getUserId(), tierCode, durationDays);
    }

    // ==================== 订单状态查询 ====================

    public VipDto.OrderStatus getOrderStatus(String orderNo) {
        VipOrder order = vipOrderMapper.selectOne(
                new LambdaQueryWrapper<VipOrder>().eq(VipOrder::getOrderNo, orderNo));
        if (order == null)
            throw new BusinessException("订单不存在");

        VipDto.OrderStatus s = new VipDto.OrderStatus();
        s.setOrderNo(order.getOrderNo());
        s.setStatus(order.getStatus());
        // paid 或 activated 都算已支付
        s.setPaid("paid".equals(order.getStatus()) || "activated".equals(order.getStatus()));
        s.setAmount(order.getAmount());
        s.setPlanName(order.getPlanName());
        return s;
    }

    // ==================== 会员状态 ====================

    public VipDto.StatusResponse getStatus(Long userId) {
        User user = userMapper.selectById(userId);
        VipDto.StatusResponse resp = new VipDto.StatusResponse();
        resp.setVip(user.isVip());
        resp.setLevel(user.getVipLevel() != null ? user.getVipLevel() : 0);
        if (user.getVipExpireTime() != null) {
            resp.setExpireTime(user.getVipExpireTime().toString());
            resp.setRemainDays((int) ChronoUnit.DAYS.between(LocalDateTime.now(), user.getVipExpireTime()));
        }
        return resp;
    }
}
