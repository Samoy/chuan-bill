package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "预算详情响应")
public class BudgetVO {

    @Schema(description = "预算ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "月份，格式 YYYY-MM")
    private String month;

    @Schema(description = "预算金额")
    private BigDecimal amount;

    @Schema(description = "已使用金额")
    private BigDecimal useAmount;

    @Schema(description = "剩余金额")
    private BigDecimal remainingAmount;

    @Schema(description = "使用百分比")
    private BigDecimal usagePercent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
