package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类统计参数
 *
 * @author samoy
 * @since 2026/4/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "分类统计参数")
public class StatisticsCategoryDTO {

    @NotBlank(message = "月份不能为空") @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式错误") @Schema(description = "月份，格式为YYYY-MM", example = "2026-04", requiredMode = RequiredMode.REQUIRED)
    private String month;

    @NotBlank(message = "类型不能为空") @Pattern(regexp = "^(income|expense)$", message = "类型必须为income或expense")
    @Schema(description = "账单类型：income-收入，expense-支出", example = "expense", requiredMode = RequiredMode.REQUIRED)
    private String type;

    @Schema(description = "家庭ID，用于查询家庭账单统计信息", example = "1234567890")
    private String familyId;
}
