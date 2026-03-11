package com.soundread.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 认证请求 DTO
 */
public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "手机号不能为空")
        private String phone;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    /**
     * 邀请码注册请求
     * 替代原短信验证码注册方案
     */
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "手机号不能为空")
        private String phone;
        @NotBlank(message = "邀请码不能为空")
        private String inviteCode;
        @NotBlank(message = "密码不能为空")
        private String password;
        @NotBlank(message = "确认密码不能为空")
        private String confirmPassword;
        private String nickname;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private Long userId;
        private String nickname;
        private boolean vip;
        private boolean admin;
        /** 动态策略配置 (功能开关 + 配额 + 资源规则) */
        private TierPolicyDto.UserPolicy policy;
    }
}
