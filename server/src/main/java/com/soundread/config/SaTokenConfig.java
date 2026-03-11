package com.soundread.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 权限配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new SaInterceptor(handle -> {
                        // 跳过 CORS 预检请求（OPTIONS），避免 Sa-Token 拦截导致 CORS 失败
                        if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                                return;
                        }

                        // 需要登录的接口
                        SaRouter.match("/api/tts/**", "/api/podcast/**", "/api/voice/**",
                                        "/api/vip/**", "/api/studio/**", "/api/creation/**",
                                        "/api/novel/**", "/api/music/**", "/api/discover/works/*/like")
                                        .check(r -> StpUtil.checkLogin());

                        // 运营后台安全防护，仅超级管理员可用
                        SaRouter.match("/api/admin/**", r -> {
                                StpUtil.checkLogin(); // 必须先登录
                                Object isAdmin = StpUtil.getSession().get("admin");
                                if (!Boolean.TRUE.equals(isAdmin)) {
                                        throw new com.soundread.common.exception.BusinessException(403, "缺少运营管理访问权限");
                                }
                        });

                })).addPathPatterns("/api/**")
                                .excludePathPatterns("/api/auth/**", "/api/discover/banners", "/api/discover/works",
                                                "/api/tts/voices", "/api/vip/plans",
                                                "/api/vip/payment/alipay-notify", // 支付宝回调无 token，必须放行
                                                "/api/studio/templates");
        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                                .allowedOriginPatterns("*")
                                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                                .allowedHeaders("*")
                                .allowCredentials(true)
                                .maxAge(3600);
        }
}
