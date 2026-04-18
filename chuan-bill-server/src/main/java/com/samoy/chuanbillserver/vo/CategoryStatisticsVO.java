package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 分类统计信息
 *
 * @author samoy
 * @since 2026/4/17
 */
@Data
@Schema(description = "分类统计信息")
public class CategoryStatisticsVO {

    @Schema(description = "分类ID", example = "123456")
    private String categoryId;

    @Schema(description = "分类名称", example = "餐饮")
    private String categoryName;

    @Schema(description = "分类图标", example = "https://example.com/icon/food.png")
    private String categoryIcon;

    @Schema(description = "金额", example = "1234.56")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @Schema(description = "占比百分比", example = "35.50")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal percentage;
}
