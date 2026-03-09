package com.soundread.controller;

import com.soundread.common.Result;
import com.soundread.model.dto.AuthDto;
import com.soundread.model.dto.TierPolicyDto;
import com.soundread.model.entity.User;
import com.soundread.service.AuthService;
import com.soundread.service.QuotaService;
import com.soundread.service.TierPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证 Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;
        private final QuotaService quotaService;
        private final TierPolicyService tierPolicyService;

        @PostMapping("/login")
        public Result<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
                return Result.ok(authService.login(req));
        }

        @PostMapping("/sms-code")
        public Result<Void> sendSmsCode(@RequestParam String phone) {
                authService.sendSmsCode(phone);
                return Result.ok();
        }

        @PostMapping("/sms-login")
        public Result<AuthDto.LoginResponse> smsLogin(@Valid @RequestBody AuthDto.SmsLoginRequest req) {
                return Result.ok(authService.smsLogin(req));
        }

        @PostMapping("/register")
        public Result<AuthDto.LoginResponse> register(@Valid @RequestBody AuthDto.RegisterRequest req) {
                return Result.ok(authService.register(req));
        }

        @GetMapping("/me")
        public Result<User> me() {
                return Result.ok(authService.getCurrentUser());
        }

        /**
         * 查询当前用户配额使用情况
         * 返回各模块的上限 + 今日已使用量
         */
        @GetMapping("/quota-usage")
        public Result<Map<String, Object>> getQuotaUsage() {
                User user = authService.getCurrentUser();
                com.soundread.model.entity.SysTierPolicy policy = tierPolicyService.getByTierCode(
                                user.getTierCode() != null ? user.getTierCode() : "user");

                TierPolicyDto.QuotaLimits limits = policy != null && policy.getQuotaLimits() != null
                                ? policy.getQuotaLimits()
                                : new TierPolicyDto.QuotaLimits();

                Map<String, Object> result = new LinkedHashMap<>();

                // TTS 1.0 基础合成每日字数
                result.put("ttsChars", Map.of(
                                "label", "基础合成",
                                "icon", "volume-up",
                                "used", quotaService.getDailyUsage(user.getId(), "text"),
                                "limit", limits.getTtsDailyChars(),
                                "unit", "字"));

                // TTS 2.0 情感合成每日字数
                result.put("ttsV2Chars", Map.of(
                                "label", "情感合成",
                                "icon", "theater-masks",
                                "used", quotaService.getDailyUsage(user.getId(), "text_v2"),
                                "limit", limits.getTtsV2DailyChars(),
                                "unit", "字"));

                // AI 编排
                result.put("aiScript", Map.of(
                                "label", "AI 编排",
                                "icon", "magic",
                                "used", quotaService.getDailyUsage(user.getId(), "ai_script"),
                                "limit", limits.getAiScriptDailyCount(),
                                "unit", "次"));

                // AI 播客
                result.put("podcast", Map.of(
                                "label", "AI 双播",
                                "icon", "podcast",
                                "used", quotaService.getDailyUsage(user.getId(), "podcast"),
                                "limit", limits.getPodcastDailyCount(),
                                "unit", "次"));

                // AI 小说创作每日字数
                result.put("novel", Map.of(
                                "label", "小说合成",
                                "icon", "book-open",
                                "used", quotaService.getDailyUsage(user.getId(), "novel"),
                                "limit", limits.getNovelDailyChars(),
                                "unit", "字"));

                // AI 音乐每日生成次数
                result.put("music", Map.of(
                                "label", "AI 音乐",
                                "icon", "music",
                                "used", quotaService.getDailyUsage(user.getId(), "music"),
                                "limit", limits.getMusicDailyCount(),
                                "unit", "次"));

                // 等级信息
                result.put("tier", Map.of(
                                "code", user.getTierCode() != null ? user.getTierCode() : "user",
                                "name", policy != null ? policy.getTierName() : "普通用户"));

                return Result.ok(result);
        }

        @PostMapping("/logout")
        public Result<Void> logout() {
                authService.logout();
                return Result.ok();
        }
}
