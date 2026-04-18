package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI分析结果")
public class AiAnalysisVO {

    @Schema(description = "AI分析内容")
    private String content;

    @Schema(description = "是否为缓存结果")
    private Boolean cached;

    @Schema(description = "今日剩余分析次数，-1表示无限制(VIP)")
    private Integer remainingCount;
}
