package com.soundread.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.soundread.common.Result;
import com.soundread.model.dto.VipDto;
import com.soundread.service.VipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VIP 会员 Controller
 *
 * <p>
 * 接口列表：
 * </p>
 * <ul>
 * <li>GET /api/vip/plans — 套餐列表</li>
 * <li>POST /api/vip/orders — 创建订单（返回支付宝跳转 URL）</li>
 * <li>POST /api/vip/payment/alipay-notify — 支付宝异步回调（验签 + 激活 VIP）</li>
 * <li>GET /api/vip/order/{orderNo}/status — 前端轮询订单状态</li>
 * <li>GET /api/vip/status — 当前用户会员状态</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;

    /** 套餐列表（公开接口，不需要登录） */
    @GetMapping("/plans")
    public Result<List<VipDto.PlanItem>> getPlans() {
        return Result.ok(vipService.getPlans());
    }

    /** 创建订单，根据设备自动选择 PC/WAP 支付 */
    @PostMapping("/orders")
    public Result<VipDto.OrderResponse> createOrder(
            @Valid @RequestBody VipDto.SubscribeRequest req,
            HttpServletRequest httpRequest) {
        long userId = StpUtil.getLoginIdAsLong();
        String ua = httpRequest.getHeader("User-Agent");
        boolean isMobile = ua != null && ua.matches(".*(Android|iPhone|iPad|Mobile|MicroMessenger).*");
        return Result.ok(vipService.createOrder(userId, req, isMobile));
    }

    /**
     * 支付宝异步通知回调
     *
     * <p>
     * ⚠️ 此接口必须：
     * </p>
     * <ul>
     * <li>绕过 Sa-Token 鉴权（支付宝服务器调用，没有 token）</li>
     * <li>返回纯文本 "success" 或 "fail"（不能包 Result 包装）</li>
     * </ul>
     */
    @PostMapping("/payment/alipay-notify")
    public String alipayNotify(HttpServletRequest httpRequest) {
        Map<String, String> params = new HashMap<>();
        httpRequest.getParameterMap().forEach((k, v) -> params.put(k, v[0]));
        log.info("[Alipay Notify] 收到回调, out_trade_no={}", params.get("out_trade_no"));
        try {
            return vipService.handleAlipayNotify(params);
        } catch (Exception e) {
            // 所有异常统一返回 fail，支付宝 25 小时内最多重试 25 次
            log.error("[Alipay Notify] 处理失败（支付宝将重试）: out_trade_no={}",
                    params.get("out_trade_no"), e);
            return "fail";
        }
    }

    /** 前端主动轮询订单状态（支付宝跳回 return_url 后调用） */
    @GetMapping("/order/{orderNo}/status")
    public Result<VipDto.OrderStatus> getOrderStatus(@PathVariable String orderNo) {
        return Result.ok(vipService.getOrderStatus(orderNo));
    }

    /** 当前用户会员状态 */
    @GetMapping("/status")
    public Result<VipDto.StatusResponse> getStatus() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(vipService.getStatus(userId));
    }
}
