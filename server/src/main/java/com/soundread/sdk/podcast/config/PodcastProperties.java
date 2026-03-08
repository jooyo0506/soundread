package com.soundread.sdk.podcast.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Podcast SDK配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "volcengine.podcast")
public class PodcastProperties {

    /**
     * App ID
     */
    private String appId;

    /**
     * Access Token
     */
    private String accessToken;

    /**
     * WebSocket端点
     */
    private String endpoint = "wss://openspeech.bytedance.com/api/v3/sami/podcasttts";

    /**
     * 资源ID
     */
    private String resourceId = "volc.service_type.10050";

    /**
     * 默认主播A音色 (女声)
     */
    private String defaultVoiceA = "zh_female_mizaitongxue_v2_saturn_bigtts";

    /**
     * 默认主播B音色 (男声)
     */
    private String defaultVoiceB = "zh_male_dayixiansheng_v2_saturn_bigtts";

    /**
     * 最大文本长度
     */
    private int maxTextLength = 10000;

    /**
     * 连接超时 (秒)
     */
    private int connectTimeout = 30;

    /**
     * 读取超时 (秒)
     */
    private int readTimeout = 300;
}
