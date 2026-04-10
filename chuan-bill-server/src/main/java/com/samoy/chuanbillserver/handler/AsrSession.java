package com.samoy.chuanbillserver.handler;

import cn.hutool.core.util.ObjectUtil;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

/**
 * <p>
 * 语音识别会话
 * </p>
 *
 * @author samoy
 * @since 2026/4/10
 */
@Slf4j
public class AsrSession {

    private final WebSocketSession webSocketSession;

    @Getter
    @Setter
    private volatile boolean recognizing = false;

    private final AtomicReference<AsrSender> asrSenderRef = new AtomicReference<>();

    public AsrSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    public String getSessionId() {
        return webSocketSession.getId();
    }

    /**
     * 设置语音识别发送器
     *
     * @param asrSender 语音识别发送器
     */
    public void setSender(AsrSender asrSender) {
        asrSenderRef.set(asrSender);
    }

    /**
     * 发送音频数据到语音识别服务
     *
     * @param byteBuffer 音频数据
     */
    public void sendAudioData(ByteBuffer byteBuffer) {
        AsrSender sender = asrSenderRef.get();
        if (ObjectUtil.isNotNull(sender)) {
            sender.send(byteBuffer);
        } else {
            log.warn("语音识别发送器未初始化");
        }
    }

    /**
     * 停止语音识别
     */
    public void stopRecognition() {
        AsrSender sender = asrSenderRef.get();
        if (ObjectUtil.isNotNull(sender)) {
            sender.stop();
        }
        recognizing = false;
    }

    /**
     * 关闭会话
     */
    public void close() {
        stopRecognition();
        asrSenderRef.set(null);
    }

    /**
     * 语音识别发送器
     */
    public interface AsrSender {
        /**
         * 发送音频数据
         *
         * @param byteBuffer 数据
         */
        void send(ByteBuffer byteBuffer);

        /**
         * 停止识别
         */
        void stop();
    }
}
