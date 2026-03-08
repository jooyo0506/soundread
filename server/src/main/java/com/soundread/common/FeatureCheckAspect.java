package com.soundread.common;

import cn.dev33.satoken.stp.StpUtil;
import com.soundread.common.exception.BusinessException;
import com.soundread.mapper.UserMapper;
import com.soundread.model.entity.User;
import com.soundread.service.TierPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 功能权限校验 AOP 切面
 * <p>
 * 拦截所有标注了 {@link RequireFeature} 的 Controller 方法,
 * 在进入方法前从缓存中取出当前用户的 tier_code,
 * 判断其 feature_flags 中对应的功能是否为 true。
 * </p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureCheckAspect {

    private final TierPolicyService tierPolicyService;
    private final UserMapper userMapper;

    @Around("@annotation(requireFeature)")
    public Object checkFeature(ProceedingJoinPoint joinPoint, RequireFeature requireFeature) throws Throwable {
        // 1. 获取当前登录用户
        if (!StpUtil.isLogin()) {
            throw new BusinessException("请先登录");
        }

        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 获取 tierCode, 查询功能开关
        String tierCode = user.getTierCode();
        String featureName = requireFeature.value();

        boolean hasAccess = tierPolicyService.hasFeature(tierCode, featureName);
        if (!hasAccess) {
            log.info("功能权限拦截: userId={}, tierCode={}, feature={}", userId, tierCode, featureName);
            throw new BusinessException(requireFeature.message());
        }

        // 3. 放行
        return joinPoint.proceed();
    }
}
