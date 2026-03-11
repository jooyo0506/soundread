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
     * 必须幂等，同一笔交易可能收到多次通知
     */
    @Transactional(rollbackFor = Exception.class)
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            // 1. 验签
            boolean signOk = AlipaySignature.rsaCheckV1(
                    params, alipayProps.getPublicKey(), "UTF-8", "RSA2");
            if (!signOk) {
                log.warn("支付宝回调签名验证失败: params={}", params);
                return "fail";
            }

            // 2. 只处理 trade_success 状态
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return "success"; // 其他状态直接返回 success（告诉支付宝不再重试）
            }

            String orderNo = params.get("out_trade_no");
            String alipayTradeNo = params.get("trade_no");

            // 3. 查订单
            VipOrder order = vipOrderMapper.selectOne(
                    new LambdaQueryWrapper<VipOrder>().eq(VipOrder::getOrderNo, orderNo));
            if (order == null) {
                log.error("回调订单不存在: orderNo={}", orderNo);
                return "fail";
            }

            // 4. 幂等：已支付不重复处理
            if ("paid".equals(order.getStatus())) {
                return "success";
            }

            // 5. 更新订单状态
            order.setStatus("paid");
            order.setAlipayTradeNo(alipayTradeNo);
            order.setPayTime(LocalDateTime.now());
            order.setNotifyRaw(params.toString());
            vipOrderMapper.updateById(order);

            // 6. 激活用户 VIP
            activateUserVip(order);

            log.info("支付宝回调处理成功: orderNo={}, alipayTradeNo={}", orderNo, alipayTradeNo);
            return "success";

        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            return "fail";
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
