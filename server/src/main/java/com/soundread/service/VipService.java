package com.soundread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soundread.common.exception.BusinessException;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * VIP 会员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipService {

    private final VipOrderMapper vipOrderMapper;
    private final UserMapper userMapper;

    /**
     * 获取套餐列表
     */
    public List<VipDto.PlanResponse> getPlans() {
        VipDto.PlanResponse month = new VipDto.PlanResponse();
        month.setPlanId("month");
        month.setName("月度体验");
        month.setPrice("¥30");
        month.setDuration("30天");
        month.setFeatures(new String[] { "无限字数合成", "情感合成/指令", "AI播客完整版" });

        VipDto.PlanResponse year = new VipDto.PlanResponse();
        year.setPlanId("year");
        year.setName("年度 VIP");
        year.setPrice("¥300");
        year.setDuration("365天");
        year.setFeatures(new String[] { "全部月度功能", "专属声音复刻", "WebSocket流式", "同声传译/翻译", "边听边问无限次" });

        VipDto.PlanResponse lifetime = new VipDto.PlanResponse();
        lifetime.setPlanId("lifetime");
        lifetime.setName("终身 VIP");
        lifetime.setPrice("¥1000");
        lifetime.setDuration("永久有效");
        lifetime.setFeatures(new String[] { "全部年度功能", "永不过期", "优先客服支持" });

        return List.of(month, year, lifetime);
    }

    /**
     * 创建支付订单
     */
    public VipDto.OrderResponse createOrder(Long userId, VipDto.SubscribeRequest req) {
        BigDecimal amount = switch (req.getPlanId()) {
            case "month" -> new BigDecimal("30.00");
            case "year" -> new BigDecimal("300.00");
            case "lifetime" -> new BigDecimal("1000.00");
            default -> throw new BusinessException("无效的套餐");
        };

        VipOrder order = new VipOrder();
        order.setUserId(userId);
        order.setPlanId(req.getPlanId());
        order.setAmount(amount);
        order.setPayMethod(req.getPayMethod());
        order.setStatus("pending");
        order.setCreatedAt(LocalDateTime.now());
        vipOrderMapper.insert(order);

        // TODO: 调用微信/支付宝支付 API 获取真实支付链接
        VipDto.OrderResponse resp = new VipDto.OrderResponse();
        resp.setOrderId(String.valueOf(order.getId()));
        resp.setPayUrl("https://pay.soundread.com/pay?orderId=" + order.getId());
        return resp;
    }

    /**
     * 支付成功回调 — 激活 VIP
     */
    // 【阿里规范】@Transactional 必须指定 rollbackFor，否则受检异常不会回滚
    @Transactional(rollbackFor = Exception.class)
    public void activateVip(Long orderId) {
        VipOrder order = vipOrderMapper.selectById(orderId);
        if (order == null || !"pending".equals(order.getStatus())) {
            return;
        }

        order.setStatus("paid");
        order.setPaidAt(LocalDateTime.now());

        // 计算过期时间
        LocalDateTime expireAt = switch (order.getPlanId()) {
            case "month" -> LocalDateTime.now().plusDays(30);
            case "year" -> LocalDateTime.now().plusDays(365);
            case "lifetime" -> LocalDateTime.of(2099, 12, 31, 23, 59);
            default -> LocalDateTime.now().plusDays(30);
        };
        order.setExpireAt(expireAt);
        vipOrderMapper.updateById(order);

        // 更新用户 VIP 状态
        User user = userMapper.selectById(order.getUserId());
        int vipLevel = switch (order.getPlanId()) {
            case "month" -> 1;
            case "year" -> 2;
            case "lifetime" -> 3;
            default -> 1;
        };
        user.setVipLevel(vipLevel);
        user.setVipExpireTime(expireAt);
        userMapper.updateById(user);

        log.info("VIP 激活成功: userId={}, plan={}", user.getId(), order.getPlanId());
    }

    /**
     * 查询会员状态
     */
    public VipDto.StatusResponse getStatus(Long userId) {
        User user = userMapper.selectById(userId);
        VipDto.StatusResponse resp = new VipDto.StatusResponse();
        resp.setVip(user.isVip());
        resp.setLevel(user.getVipLevel());
        if (user.getVipExpireTime() != null) {
            resp.setExpireTime(user.getVipExpireTime().toString());
            resp.setRemainDays((int) ChronoUnit.DAYS.between(LocalDateTime.now(), user.getVipExpireTime()));
        }
        return resp;
    }
}
