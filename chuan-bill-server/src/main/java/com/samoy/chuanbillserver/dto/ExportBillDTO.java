package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "账单导出参数")
public class ExportBillDTO {

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式错误") @Schema(description = "开始日期", example = "2024-01-01")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式错误") @Schema(description = "结束日期", example = "2024-03-31")
    private String endDate;

    @Schema(description = "分类 ID", example = "123456")
    private String categoryId;

    @Pattern(regexp = "^(income|expense|)$", message = "类型不正确")
    @Schema(description = "账单类型：income-收入，expense-支出，空字符串：全部", example = "expense")
    private String type;

    @Schema(description = "支付方式 ID", example = "123456")
    private String paymentMethodId;

    @Schema(description = "家庭 ID", example = "family123")
    private String familyId;

    @NotBlank(message = "导出格式不能为空") @Pattern(regexp = "^(excel|pdf)$", message = "格式仅支持 excel 或 pdf")
    @Schema(description = "导出格式：excel 或 pdf", example = "excel", requiredMode = Schema.RequiredMode.REQUIRED)
    private String format;
}
