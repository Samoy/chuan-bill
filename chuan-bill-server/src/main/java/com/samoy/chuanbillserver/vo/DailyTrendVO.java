package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 每日收支趋势
 *
 * @author samoy
 * @since 2026/4/17
 */
@Data
@Schema(description = "每日收支趋势")
public class DailyTrendVO {

    @Schema(description = "日期，格式为YYYY-MM-DD", example = "2026-04-01")
    private String date;

    @Schema(description = "支出金额", example = "123.45")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal expense;

    @Schema(description = "收入金额", example = "456.78")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal income;
}
