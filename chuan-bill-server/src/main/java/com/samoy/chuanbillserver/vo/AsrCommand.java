package com.samoy.chuanbillserver.vo;

import lombok.Data;

/**
 * <p>
 * 语音识别服务WebSocket命令
 * </p>
 *
 * @author samoy
 * @since 2026/4/10
 */
@Data
public class AsrCommand {
    private String action;
    private String format;
    private Integer sampleRate;
    private String[] languageHints;
}
