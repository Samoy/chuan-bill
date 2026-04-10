package com.samoy.chuanbillserver.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * <p>
 * WebSocket消息
 * </p>
 *
 * @author samoy
 * @since 2026/4/10
 */
@Data
@AllArgsConstructor
public class WebSocketMessage<T> {
    private String type;
    private String message;
    private T data;
}
