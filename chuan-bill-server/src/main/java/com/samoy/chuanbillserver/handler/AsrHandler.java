package com.samoy.chuanbillserver.handler;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samoy.chuanbillserver.service.IAsrService;
import com.samoy.chuanbillserver.vo.AsrCommand;
import com.samoy.chuanbillserver.vo.AsrResultVO;
import com.samoy.chuanbillserver.vo.WebSocketMessage;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * <p>
 * 语音识别处理器
 * </p>
 *
 * @author samoy
 * @since 2026/4/10
 */
@Slf4j
@Component
public class AsrHandler extends AbstractWebSocketHandler {
    @Resource
    private IAsrService asrService;

    private final ObjectMapper objectMapper;

    private final Map<String, AsrSession> asrSessionMap = new ConcurrentHashMap<>();

    public AsrHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("WebSocket 连接建立 sessionId={}", sessionId);
        AsrSession asrSession = new AsrSession(session);
        asrSessionMap.put(sessionId, asrSession);
        sendMessage(session, new WebSocketMessage<>("connected", "语音识别服务已连接", null));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到文本消息: {}", payload);
        try {
            AsrCommand command = objectMapper.readValue(payload, AsrCommand.class);
            AsrSession asrSession = asrSessionMap.get(session.getId());
            switch (command.getAction()) {
                case "start":
                    handleStartRecognition(session, asrSession, command);
                    break;
                case "stop":
                    handleStopRecognition(session, asrSession);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("处理文本消息时出错", e);
            sendMessage(session, new WebSocketMessage<>("error", "消息处理失败:" + e.getMessage(), null));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        ByteBuffer payload = message.getPayload();
        AsrSession asrSession = asrSessionMap.get(session.getId());
        if (ObjectUtil.isNull(asrSession) || !asrSession.isRecognizing()) {
            sendMessage(session, new WebSocketMessage<>("error", "语音识别未开始", null));
            return;
        }
        try {
            asrSession.sendAudioData(payload);
        } catch (Exception e) {
            log.error("发送音频数据时出错", e);
            sendMessage(session, new WebSocketMessage<>("error", "音频处理失败:" + e.getMessage(), null));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        log.info("WebSocket 连接关闭 sessionId={}, status={}", sessionId, status);

        AsrSession asrSession = asrSessionMap.remove(sessionId);
        if (ObjectUtil.isNotNull(asrSession)) {
            asrSession.close();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误 sessionId={}", session.getId(), exception);
        AsrSession asrSession = asrSessionMap.get(session.getId());
        if (ObjectUtil.isNotNull(asrSession)) {
            asrSession.close();
        }
    }

    private void handleStartRecognition(WebSocketSession session, AsrSession asrSession, AsrCommand command)
            throws IOException {
        try {
            String format = ObjectUtil.isNotEmpty(command.getFormat()) ? command.getFormat() : "pcm";
            int sampleRate = ObjectUtil.isNotEmpty(command.getSampleRate()) ? command.getSampleRate() : 16000;
            String[] languageHints = ObjectUtil.isNotEmpty(command.getLanguageHints())
                    ? command.getLanguageHints()
                    : new String[] {"zh"};

            // 初始化语音识别
            asrService.startAsr(asrSession, format, sampleRate, languageHints, new IAsrService.AsrCallback() {
                @Override
                public void onResult(RecognitionResult result) {
                    handleAsrResult(session, result);
                }

                @Override
                public void onError(Exception e) {
                    handleAsrError(session, e);
                }

                @Override
                public void onCompleted() {
                    handleAsrCompleted(session);
                }
            });
            asrSession.setRecognizing(true);
            sendMessage(session, new WebSocketMessage<>("started", "语音识别已开始", null));
        } catch (IOException e) {
            log.error("启动语音识别失败", e);
            sendMessage(session, new WebSocketMessage<>("error", "启动语音识别失败:" + e.getMessage(), null));
        }
    }

    private void handleStopRecognition(WebSocketSession session, AsrSession asrSession) {
        try {
            asrSession.stopRecognition();
            asrSession.setRecognizing(false);
            sendMessage(session, new WebSocketMessage<>("stopped", "语音识别已停止", null));
        } catch (Exception e) {
            log.error("处理语音识别停止时出错", e);
            try {
                sendMessage(session, new WebSocketMessage<>("error", "语音识别停止时出错:" + e.getMessage(), null));
            } catch (IOException ex) {
                log.error("发送错误消息时出错", ex);
            }
        }
    }

    private void handleAsrResult(WebSocketSession session, RecognitionResult result) {
        AsrResultVO asrResultVO = new AsrResultVO();
        asrResultVO.setText(result.getSentence().getText());
        asrResultVO.setSentenceEnd(result.isSentenceEnd());
        asrResultVO.setBeginTime(result.getSentence().getBeginTime());
        asrResultVO.setEndTime(result.getSentence().getEndTime());
        try {
            sendMessage(session, new WebSocketMessage<>("result", null, asrResultVO));
        } catch (IOException e) {
            log.error("处理语音识别结果时出错", e);
        }
    }

    private void handleAsrError(WebSocketSession session, Exception e) {
        try {
            sendMessage(session, new WebSocketMessage<>("error", "识别错误：" + e.getMessage(), null));
        } catch (IOException ex) {
            log.error("处理语音识别错误时出错", ex);
        }
    }

    private void handleAsrCompleted(WebSocketSession session) {
        try {
            sendMessage(session, new WebSocketMessage<>("completed", "识别完成", null));
        } catch (IOException e) {
            log.error("处理语音识别完成时出错", e);
        }
    }

    public void sendMessage(WebSocketSession session, WebSocketMessage<AsrResultVO> message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }
}
