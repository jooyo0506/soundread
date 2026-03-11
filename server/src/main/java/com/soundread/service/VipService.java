package com.soundread.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VIP 会员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipService {

    private final VipOrderMapper vipOrderMapper;
    private final UserMapper userMapper;
    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProps;
    @Lazy
    private final AuthService authService;

    // ==================== 套餐配置 ====================

    private static final List<VipDto.PlanItem> PLANS = List.of(
            new VipDto.PlanItem("vip_month", "月度体验", new BigDecimal("30.00"), 30, null),
            new VipDto.PlanItem("vip_year", "年度 VIP", new BigDecimal("300.00"), 365, new BigDecimal("360.00")),
            new VipDto.PlanItem("vip_lifetime", "终身 VIP", new BigDecimal("1000.00"), 9999, null));

    public List<VipDto.PlanItem> getPlans() {
        return PLANS;
    }

    // ==================== 创建订单 + 获取支付宝跳转 URL ====================

    @Transactional(rollbackFor = Exception.class)
    public VipDto.OrderResponse createOrder(Long userId, VipDto.SubscribeRequest req, boolean isMobile) {
        VipDto.PlanItem plan = PLANS.stream()
                .filter(p -> p.getPlanId().equals(req.getPlanId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("无效的套餐: " + req.getPlanId()));

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
     * mobile=true → WAP 手机网站支付
     * mobile=false → Page PC 网页支付
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
     * 处理支付宝异步通知
     *
     * <p>
     * 三道可靠性防线：
     * </p>
     * <ol>
     * <li>RSA2 验签 — 防伪造回调</li>
     * <li>DB CAS 原子更新 — UPDATE WHERE status='pending'，防并发双写（高并发下多个 notify
     * 同时到达，MySQL 行锁保证只有1个成功）</li>
     * <li>@Transactional — activateUserVip 失败自动回滚 status 回 pending，支付宝重试时可再次激活</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            // ===== 1. 验签（防伪造） =====
            boolean signOk = AlipaySignature.rsaCheckV1(
                    params, alipayProps.getPublicKey(), "UTF-8", "RSA2");
            if (!signOk) {
                log.warn("[VipNotify] 签名验证失败: params={}", params);
                return "fail";
            }

            // ===== 2. 只处理 TRADE_SUCCESS / TRADE_FINISHED =====
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return "success"; // 其他状态告知支付宝不再重试
            }

            String orderNo = params.get("out_trade_no");
            String alipayTradeNo = params.get("trade_no");

            // ===== 3. 查订单（确认存在）=====
            VipOrder order = vipOrderMapper.selectOne(
                    new LambdaQueryWrapper<VipOrder>().eq(VipOrder::getOrderNo, orderNo));
            if (order == null) {
                log.error("[VipNotify] 订单不存在: orderNo={}", orderNo);
                return "fail";
            }

            // ===== 4. ★ CAS 原子更新（核心并发防护）=====
            // UPDATE vip_order SET status='paid' WHERE order_no=? AND status='pending'
            // MySQL 行锁保证并发 notify 中只有1个线程 updated=1，其余为0
            int updated = vipOrderMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<VipOrder>()
                            .eq(VipOrder::getOrderNo, orderNo)
                            .eq(VipOrder::getStatus, "pending") // CAS 条件
                            .set(VipOrder::getStatus, "paid")
                            .set(VipOrder::getAlipayTradeNo, alipayTradeNo)
                            .set(VipOrder::getPayTime, LocalDateTime.now())
                            .set(VipOrder::getNotifyRaw, params.toString()));

            if (updated == 0) {
                // 已被其他线程处理（并发重试）→ 幂等返回成功
                log.info("[VipNotify] 已处理，幂等跳过: orderNo={}", orderNo);
                return "success";
            }

            // ===== 5. 激活 VIP（在事务内，失败会回滚 status 回 pending，支付宝可继续重试）=====
            VipOrder paid = vipOrderMapper.selectOne(
                    new LambdaQueryWrapper<VipOrder>().eq(VipOrder::getOrderNo, orderNo));
            activateUserVip(paid);

            log.info("[VipNotify] 处理成功: orderNo={}, alipayTradeNo={}", orderNo, alipayTradeNo);
            return "success";

        } catch (Exception e) {
            log.error("[VipNotify] 处理异常（事务回滚，支付宝将重试）", e);
            return "fail"; // 支付宝 25 小时内最多重试 25 次
        }
    }

    /**
     * 激活用户 VIP（更新 User 表）
     */
    private void activateUserVip(VipOrder order) {
        User user = userMapper.selectById(order.getUserId());
        if (user == null)
            return;

        LocalDateTime expireAt = "vip_lifetime".equals(order.getPlanId())
                ? LocalDateTime.of(2099, 12, 31, 23, 59)
                : LocalDateTime.now().plusDays(order.getDurationDays());

        // 已有 VIP 则叠加时间
        if (user.isVip() && user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now())) {
            expireAt = user.getVipExpireTime().plusDays(order.getDurationDays());
        }

        int vipLevel = switch (order.getPlanId()) {
            case "vip_month" -> 1;
            case "vip_year" -> 2;
            case "vip_lifetime" -> 3;
            default -> 1;
        };

        // 取高等级（购买年度时不降级已有终身）
        if (user.getVipLevel() != null && user.getVipLevel() > vipLevel) {
            vipLevel = user.getVipLevel();
        }

        // 更新等级策略 tierCode
        String tierCode = switch (vipLevel) {
            case 2 -> "vip_year";
            case 3 -> "vip_lifetime";
            default -> "vip_month";
        };

        user.setVipLevel(vipLevel);
        user.setVipExpireTime(expireAt);
        user.setTierCode(tierCode);
        userMapper.updateById(user);

        // ★ P2-a 缓存清除：VIP 激活后清除 Redis 用户缓存，保证下次请求读到最新 VIP 状态
        authService.evictUserCache(user.getId());

        log.info("VIP 激活: userId={}, tierCode={}, expireAt={}", user.getId(), tierCode, expireAt);
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
        s.setPaid("paid".equals(order.getStatus()));
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
