package com.soundread.config;

import com.soundread.websocket.PodcastWebSocketHandler;
import com.soundread.websocket.InteractionWebSocketHandler;
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
    private final InteractionWebSocketHandler interactionWebSocketHandler;

    public WebSocketConfig(PodcastWebSocketHandler podcastWebSocketHandler,
            InteractionWebSocketHandler interactionWebSocketHandler) {
        this.podcastWebSocketHandler = podcastWebSocketHandler;
        this.interactionWebSocketHandler = interactionWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(podcastWebSocketHandler, "/ws/podcast")
                .addHandler(interactionWebSocketHandler, "/ws/interaction")
                .setAllowedOrigins("*");
    }
}
