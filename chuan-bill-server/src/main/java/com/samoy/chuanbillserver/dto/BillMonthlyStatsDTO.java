package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "账单月度统计参数")
public class BillMonthlyStatsDTO {

    @NotBlank(message = "月份不能为空") @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式错误") @Schema(description = "月份，格式为YYYY-MM", example = "2023-04", requiredMode = Schema.RequiredMode.REQUIRED)
    private String month;

    @Schema(description = "家庭ID，用于查询家庭账单统计信息", example = "1234567890")
    private String familyId;
}
