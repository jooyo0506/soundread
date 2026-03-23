package com.soundread.job;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.soundread.model.entity.VipOrder;
import com.soundread.mapper.VipOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 超时订单自动关闭
 *
 * <p>
 * 每 10 分钟扫描超过 30 分钟仍为 pending 的订单，标记为 expired。
 * 清理垃圾订单，防止用户重复下单导致大量 pending 堆积。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final VipOrderMapper vipOrderMapper;

    /** 订单超时时间：30 分钟 */
    private static final int TIMEOUT_MINUTES = 30;

    /**
     * 定时关闭超时未支付订单
     *
     * <p>
     * 使用 CAS 条件 status='pending'，只关闭未支付的订单，
     * 不会影响已支付或已激活的订单。
     * </p>
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 每 10 分钟
    public void closeExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        int closed = vipOrderMapper.update(null,
                new LambdaUpdateWrapper<VipOrder>()
                        .eq(VipOrder::getStatus, "pending")
                        .lt(VipOrder::getCreatedAt, deadline)
                        .set(VipOrder::getStatus, "expired"));

        if (closed > 0) {
            log.info("[OrderTimeout] 关闭 {} 笔超时订单（超过 {}min 未支付）", closed, TIMEOUT_MINUTES);
        }
    }
}
