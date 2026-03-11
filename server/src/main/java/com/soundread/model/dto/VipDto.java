package com.soundread.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * VIP 相关 DTO 集合
 */
public class VipDto {

    /** 订阅请求：只需 planId，支付方式固定为支付宝 */
    @Data
    public static class SubscribeRequest {
        @NotBlank
        private String planId; // vip_month / vip_year / vip_lifetime
    }

    /** 套餐详情（面向前端展示） */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlanItem {
        private String planId;
        private String name;
        private BigDecimal price;
        private Integer durationDays;
        private BigDecimal originalPrice; // 促销划线价，null=无折扣
    }

    /** 创建订单响应：返回系统订单号 + 支付宝跳转 URL */
    @Data
    public static class OrderResponse {
        private String orderNo;
        private String payUrl;
    }

    /** 订单状态查询响应 */
    @Data
    public static class OrderStatus {
        private String orderNo;
        private String status; // pending / paid / failed / refunded
        private boolean paid;
        private BigDecimal amount;
        private String planName;
    }

    /** 会员状态响应 */
    @Data
    public static class StatusResponse {
        private boolean vip;
        private Integer level;
        private String expireTime;
        private Integer remainDays;
    }
}
