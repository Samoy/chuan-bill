package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * <p>
 * 家庭成员统计请求
 * </p>
 *
 * @author samoy
 * @since 2026/4/21
 */
@Data
@Schema(description = "家庭成员统计请求")
public class FamilyMemberStatsDTO {

    @NotBlank(message = "月份不能为空") @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式必须为YYYY-MM") @Schema(description = "月份，格式为YYYY-MM", example = "2026-04", requiredMode = Schema.RequiredMode.REQUIRED)
    private String month;

    @NotBlank(message = "家庭ID不能为空") @Schema(description = "家庭ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String familyId;
}
