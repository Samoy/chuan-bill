package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Schema(description = "账单月度统计信息")
public class BillMonthlyStatsVO {
    @Schema(description = "月份，格式为YYYY-MM", example = "2023-04")
    private String month;

    @Schema(description = "支出金额", example = "123.45")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal expense;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "收入金额", example = "123.45")
    private BigDecimal income;

    @Schema(description = "结余金额", example = "123.45")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal balance;
}
