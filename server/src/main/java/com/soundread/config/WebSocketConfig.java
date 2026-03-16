package com.soundread.config;

import com.soundread.websocket.PodcastWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点注册
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PodcastWebSocketHandler podcastWebSocketHandler;

    public WebSocketConfig(PodcastWebSocketHandler podcastWebSocketHandler) {
        this.podcastWebSocketHandler = podcastWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(podcastWebSocketHandler, "/ws/podcast")
                .setAllowedOrigins("*");
    }
}
