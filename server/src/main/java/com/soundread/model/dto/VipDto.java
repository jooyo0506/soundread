package com.soundread.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * VIP 订单 DTO
 */
public class VipDto {

    @Data
    public static class SubscribeRequest {
        /** month / year / lifetime */
        @NotBlank
        private String planId;
        /** wechat / alipay */
        @NotBlank
        private String payMethod;
    }

    @Data
    public static class OrderResponse {
        private String orderId;
        private String payUrl;
    }

    @Data
    public static class StatusResponse {
        private boolean vip;
        private Integer level;
        private String expireTime;
        private Integer remainDays;
    }

    @Data
    public static class PlanResponse {
        private String planId;
        private String name;
        private String price;
        private String duration;
        private String[] features;
    }
}
