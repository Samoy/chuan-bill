package com.samoy.chuanbillserver.config;

import com.samoy.chuanbillserver.handler.AsrHandler;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * <p>
 * WebSocket 配置类
 * </p>
 *
 * @author samoy
 * @since 2026/4/10
 */
@EnableWebSocket
@Configuration
public class WebsocketConfig implements WebSocketConfigurer {

    @Resource
    private AsrHandler asrHandler;

    @Resource
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(asrHandler, "/asr")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*");
    }
}
