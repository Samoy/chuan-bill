package com.samoy.chuanbillserver.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 语音识别结果
 * </p>
 *
 * @author samoy
 * @since 2026/4/10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AsrResultVO {
    private String text;
    private boolean sentenceEnd;
    private Long beginTime;
    private Long endTime;
}
