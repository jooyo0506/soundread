package com.soundread.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置项
 * 对应 application-prod.yml 中的 alipay.* 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /** 支付宝开放平台应用 ID */
    private String appId;

    /** 应用私钥（RSA2，PKCS8 格式） */
    private String privateKey;

    /** 支付宝公钥（验签用，从控制台获取） */
    private String publicKey;

    /** 支付宝网关（固定值） */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

    /** 异步通知 URL（公网可达，必须 HTTPS） */
    private String notifyUrl;

    /** 同步跳回 URL（支付完成后跳回前端页面） */
    private String returnUrl;
}
