package com.samoy.chuanbillserver.config;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * <p>
 * WebSocket 认证拦截器
 * </p>
 *
 * @author samoy
 * @since 2026/4/10
 */
@Component
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes)
            throws Exception {
        // 从请求参数中获取token
        try {
            if (request instanceof ServletServerHttpRequest serverHttpRequest) {
                String token = serverHttpRequest.getServletRequest().getParameter("token");
                if (ObjectUtil.isEmpty(token)) {
                    log.error("WebSocket 认证失败: token 为空");
                    return false;
                }
                Object userId = StpUtil.getLoginIdByToken(token);
                if (ObjectUtil.isEmpty(userId)) {
                    log.error("WebSocket 认证失败: token 无效");
                    return false;
                }
                // 将用户信息存入 WebSocket 会话
                attributes.put("userId", userId);
                attributes.put("token", token);
                log.info("WebSocket 认证成功: userId {}", userId);
                return true;
            }
        } catch (Exception e) {
            log.error("WebSocket 认证失败: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        log.info("WebSocket 连接结束");
    }
}
