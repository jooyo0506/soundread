package com.soundread.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.soundread.common.Result;
import com.soundread.model.dto.VipDto;
import com.soundread.service.VipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * VIP 会员 Controller
 */
@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;

    @GetMapping("/plans")
    public Result<List<VipDto.PlanResponse>> getPlans() {
        return Result.ok(vipService.getPlans());
    }

    @PostMapping("/orders")
    public Result<VipDto.OrderResponse> createOrder(@Valid @RequestBody VipDto.SubscribeRequest req) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(vipService.createOrder(userId, req));
    }

    @PostMapping("/callback/wechat")
    public String wechatCallback(@RequestBody String body) {
        // TODO: 验证微信签名，解析 orderId
        // vipService.activateVip(orderId);
        return "SUCCESS";
    }

    @GetMapping("/status")
    public Result<VipDto.StatusResponse> getStatus() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(vipService.getStatus(userId));
    }
}
