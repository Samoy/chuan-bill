package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>
 * 家庭AI建议
 * </p>
 *
 * @author samoy
 * @since 2026/4/21
 */
@Data
@Schema(description = "家庭AI建议")
public class FamilyAiSuggestionVO {

    @Schema(description = "建议内容")
    private String content;

    @Schema(description = "是否来自缓存")
    private Boolean cached;

    @Schema(description = "今日剩余次数")
    private Integer remainingCount;
}
