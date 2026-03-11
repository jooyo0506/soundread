package com.soundread.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝 SDK 配置
 * 初始化 AlipayClient 单例 Bean，供整个应用复用
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AlipayConfig {

    private final AlipayProperties props;

    @Bean
    public AlipayClient alipayClient() {
        log.info("初始化 AlipayClient, appId={}", props.getAppId());
        return new DefaultAlipayClient(
                props.getGatewayUrl(),
                props.getAppId(),
                props.getPrivateKey(),
                "json",
                "UTF-8",
                props.getPublicKey(),
                "RSA2");
    }
}
