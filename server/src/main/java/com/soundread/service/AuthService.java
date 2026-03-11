package com.soundread.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soundread.common.exception.BusinessException;
import com.soundread.mapper.UserMapper;
import com.soundread.model.dto.AuthDto;
import com.soundread.model.dto.TierPolicyDto;
import com.soundread.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务 (Sa-Token + JWT)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final TierPolicyService tierPolicyService;

    private static final String SMS_CODE_PREFIX = "sms:code:";

    /**
     * 密码登录
     */
    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!DigestUtils.sha256Hex(req.getPassword()).equals(user.getPasswordHash())) {
            throw new BusinessException("密码错误");
        }
        return doLogin(user);
    }

    /**
     * 验证码登录/注册
     * - 已有用户: 验证码通过后直接登录
     * - 新用户: 验证码通过后需提供密码 + 确认密码完成注册
     */
    public AuthDto.LoginResponse smsLogin(AuthDto.SmsLoginRequest req) {
        String cachedCode = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + req.getPhone());
        if (cachedCode == null || !cachedCode.equals(req.getCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        redisTemplate.delete(SMS_CODE_PREFIX + req.getPhone());

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (user == null) {
            // 新用户注册：必须提供密码和确认密码
            if (req.getPassword() == null || req.getPassword().isBlank()) {
                throw new BusinessException("请设置密码");
            }
            if (req.getConfirmPassword() == null || req.getConfirmPassword().isBlank()) {
                throw new BusinessException("请确认密码");
            }
            if (!req.getPassword().equals(req.getConfirmPassword())) {
                throw new BusinessException("两次输入的密码不一致");
            }

            user = new User();
            user.setPhone(req.getPhone());
            user.setPasswordHash(DigestUtils.sha256Hex(req.getPassword()));
            user.setNickname("声读用户" + req.getPhone().substring(7));
            user.setVipLevel(0);
            user.setTierCode("user"); // 新用户默认等级
            user.setRole("user"); // 默认角色
            user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);
            log.info("新用户注册: phone={}", req.getPhone());
        }
        return doLogin(user);
    }

    /**
     * 注册
     */
    public AuthDto.LoginResponse register(AuthDto.RegisterRequest req) {
        // 校验两次密码是否一致
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (count > 0) {
            throw new BusinessException("该手机号已注册");
        }

        User user = new User();
        user.setPhone(req.getPhone());
        user.setPasswordHash(DigestUtils.sha256Hex(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : "声读用户");
        user.setVipLevel(0);
        user.setTierCode("user");
        user.setRole("user");
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);

        return doLogin(user);
    }

    /**
     * 发送短信验证码 (模拟)
     */
    public void sendSmsCode(String phone) {
        // String code = String.valueOf((int) (Math.random() * 9000) + 1000);
        String code = "123456";// 自测
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        log.info("短信验证码发送: {} -> {}", phone, code);
        // TODO: 接入真实短信服务 (阿里云/腾讯云)
    }

    /**
     * 获取当前登录用户
     */
    public User getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        return userMapper.selectById(userId);
    }

    /**
     * 退出登录
     */
    public void logout() {
        StpUtil.logout();
    }

    private AuthDto.LoginResponse doLogin(User user) {
        StpUtil.login(user.getId());

        // 为 VIP 用户设置角色
        if (user.isVip()) {
            StpUtil.getSession().set("role", "vip");
        }

        // 识别管理员
        if ("admin".equals(user.getRole())) {
            StpUtil.getSession().set("admin", true);
        }

        AuthDto.LoginResponse resp = new AuthDto.LoginResponse();
        resp.setToken(StpUtil.getTokenValue());
        resp.setUserId(user.getId());
        resp.setNickname(user.getNickname());
        resp.setVip(user.isVip());
        resp.setAdmin("admin".equals(user.getRole()));

        // 一次性下发策略配置
        TierPolicyDto.UserPolicy policy = tierPolicyService.buildUserPolicy(
                user.getTierCode() != null ? user.getTierCode() : "user");
        resp.setPolicy(policy);

        return resp;
    }

    /**
     * 构建 /me 接口响应（含实时从 DB 加载的最新 policy，不触发登录）
     * 运营端修改 tier_code 后，下次路由守卫调用 /me 时自动同步，无需重新登录
     */
    public AuthDto.LoginResponse buildMeResponse() {
        User user = getCurrentUser();
        AuthDto.LoginResponse resp = new AuthDto.LoginResponse();
        resp.setToken(StpUtil.getTokenValue());
        resp.setUserId(user.getId());
        resp.setNickname(user.getNickname());
        resp.setVip(user.isVip());
        resp.setAdmin("admin".equals(user.getRole()));
        // 实时从 DB 加载（绕过缓存，保证运营端修改立即生效）
        TierPolicyDto.UserPolicy policy = tierPolicyService.buildUserPolicy(
                user.getTierCode() != null ? user.getTierCode() : "user");
        resp.setPolicy(policy);
        return resp;
    }
}
