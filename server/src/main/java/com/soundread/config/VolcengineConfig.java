package com.soundread.config;

import com.soundread.sdk.VolcengineSdk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 火山引擎大模型 SDK 配置装配
 */
@Configuration
public class VolcengineConfig {

    @Value("${volcengine.tts.app-id}")
    private String appId;

    @Value("${volcengine.tts.access-token}")
    private String accessToken;

    @Value("${volcengine.tts.cluster:volcano_tts}")
    private String cluster;

    @Bean
    public VolcengineSdk volcengineSdk() {
        return VolcengineSdk.builder()
                .appId(appId)
                .accessToken(accessToken)
                .cluster(cluster)
                .build();
    }
}
