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

    @Data
    public static class SmsLoginRequest {
        @NotBlank(message = "手机号不能为空")
        private String phone;
        @NotBlank(message = "验证码不能为空")
        private String code;
        /** 新用户注册时必填 */
        private String password;
        /** 新用户注册时必填 */
        private String confirmPassword;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "手机号不能为空")
        private String phone;
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
