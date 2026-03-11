package com.soundread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 配置
 *
 * <h3>企业级缓存一致性方案</h3>
 * <p>
 * 注册 {@link RedisMessageListenerContainer} Bean，用于：
 * <ul>
 * <li>订阅 {@code soundread:policy:refresh} 频道</li>
 * <li>运营端修改策略后发布消息，所有节点（含生产服务器）收到通知后清除本地 L1 缓存</li>
 * <li>避免双层缓存（JVM L1 + Redis L2）不一致的问题</li>
 * </ul>
 * </p>
 *
 * <h3>企业级标准对比</h3>
 * 
 * <pre>
 * 本项目方案：Redis Pub/Sub（秒级，适合中小规模）
 * 大厂方案：Apollo/Nacos 配置中心 → 长连接推送 → 毫秒级，多数据中心
 * </pre>
 */
@Configuration
public class RedisConfig {

    /**
     * Redis 消息监听容器
     * 支持多个 Listener 订阅不同频道，由 TierPolicyService 注册策略刷新监听器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
