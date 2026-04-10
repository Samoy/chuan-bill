package com.samoy.chuanbillserver.service;

import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.samoy.chuanbillserver.handler.AsrSession;

/**
 * <p>
 * 语音识别服务接口
 * </p>
 *
 * @author samoy
 * @since 2026/4/10
 */
public interface IAsrService {

    /**
     * 开始语音识别
     *
     * @param session       会话
     * @param format        音频格式
     * @param sampleRate    采样率
     * @param languageHints 语言提示
     * @param callback      回调
     */
    void startAsr(AsrSession session, String format, Integer sampleRate, String[] languageHints, AsrCallback callback);

    interface AsrCallback {

        /**
         * 识别结果回调
         *
         * @param result 识别结果
         */
        void onResult(RecognitionResult result);

        /**
         * 识别错误回调
         *
         * @param e 错误信息
         */
        void onError(Exception e);

        /**
         * 识别完成回调
         */
        void onCompleted();
    }
}
