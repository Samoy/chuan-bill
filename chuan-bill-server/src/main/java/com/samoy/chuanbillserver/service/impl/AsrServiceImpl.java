package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.samoy.chuanbillserver.handler.AsrSession;
import com.samoy.chuanbillserver.service.IAsrService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.disposables.Disposable;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 语音识别服务实现类
 * </p>
 *
 * @author samoy
 * @since 2026/4/10
 */
@Service
@Slf4j
public class AsrServiceImpl implements IAsrService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Override
    public void startAsr(
            AsrSession session, String format, Integer sampleRate, String[] languageHints, AsrCallback callback) {
        try {
            // 创建语音识别参数
            RecognitionParam param = RecognitionParam.builder()
                    .apiKey(apiKey)
                    .model("paraformer-realtime-v2")
                    .format(format)
                    .sampleRate(sampleRate)
                    .parameter("language_hints", languageHints)
                    .build();

            // 创建识别器
            Recognition recognition = new Recognition();

            FlowableAudioSender sender = new FlowableAudioSender();
            session.setSender(sender);

            Flowable<ByteBuffer> audioSource = Flowable.create(sender::setEmitter, BackpressureStrategy.BUFFER);
            Disposable disposable = recognition
                    .streamCall(param, audioSource)
                    .subscribe(
                            result -> {
                                log.debug("收到识别结果: {}", result.getSentence().getText());
                                callback.onResult(result);
                            },
                            e -> {
                                log.error("语音识别错误: {}", e.getMessage());
                                callback.onError(e instanceof Exception error ? error : new RuntimeException(e));
                            },
                            () -> {
                                log.info("语音识别完成");
                                callback.onCompleted();
                                closeRecognition(recognition);
                            });

            sender.setDisposable(disposable);
            sender.setRecognition(recognition);

            log.info("语音识别已启动, sessionId: {}", session.getSessionId());

        } catch (Exception e) {
            log.error("启动语音识别失败", e);
        }
    }

    private void closeRecognition(Recognition recognition) {
        try {
            recognition.getDuplexApi().close(1000, "Recognition ended");
        } catch (Exception e) {
            log.error("关闭识别器失败", e);
        }
    }

    private static class FlowableAudioSender implements AsrSession.AsrSender {

        @Setter
        private FlowableEmitter<ByteBuffer> emitter;

        @Setter
        private Disposable disposable;

        @Setter
        private Recognition recognition;

        private final AtomicBoolean running = new AtomicBoolean(false);

        @Override
        public void send(ByteBuffer audioData) {
            // 实现发送音频数据的逻辑
            if (!running.get()) return;

            if (emitter == null || emitter.isCancelled()) {
                log.warn("FlowableEmitter未初始化或者已取消");
                return;
            }
            try {
                ByteBuffer copy = ByteBuffer.allocate(audioData.remaining());
                copy.put(audioData);
                copy.flip();

                emitter.onNext(copy);
            } catch (Exception e) {
                log.error("发送音频数据失败", e);
            }
        }

        @Override
        public void stop() {
            if (!running.compareAndSet(true, false)) {
                return;
            }

            // 通知Flowable结果
            if (ObjectUtil.isNotNull(emitter) && !emitter.isCancelled()) {
                try {
                    emitter.onComplete();
                } catch (Exception e) {
                    log.error("通知Flowable结果失败", e);
                }
            }

            // 释放识别器资源
            if (ObjectUtil.isNotNull(disposable) && !disposable.isDisposed()) {
                try {
                    disposable.dispose();
                } catch (Exception e) {
                    log.error("释放识别器资源失败", e);
                }
            }
        }
    }
}
