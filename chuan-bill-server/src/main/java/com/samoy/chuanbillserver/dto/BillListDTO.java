package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Schema(description = "账单列表查询参数")
public class BillListDTO {
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式错误") @Schema(description = "开始日期", example = "2024-01-01")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式错误") @Schema(description = "结束日期", example = "2024-01-31")
    private String endDate;

    @Schema(description = "分类 ID", example = "123456")
    private String categoryId;

    @Pattern(regexp = "^(income|expense)$", message = "类型只能是 income 或 expense")
    @Schema(description = "账单类型：income-收入，expense-支出", example = "expense")
    private String type;

    @DecimalMin(value = "0.00", message = "最小金额不能小于 0.00", inclusive = false) @Digits(integer = 10, fraction = 2, message = "最小金额格式错误，最多 10 位整数，2 位小数") @Schema(description = "最小金额", example = "10.00")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.00", message = "最大金额不能小于 0.00", inclusive = false) @Digits(integer = 10, fraction = 2, message = "最大金额格式错误，最多 10 位整数，2 位小数") @Schema(description = "最大金额", example = "1000.00")
    private BigDecimal maxAmount;

    @Size(max = 50, message = "账单名称长度不能超过 50 个字符") @Schema(description = "账单名称模糊搜索", example = "早餐")
    private String name;

    @Size(max = 500, message = "账单备注长度不能超过 500 个字符") @Schema(description = "账单备注模糊搜索", example = "公司")
    private String remark;

    @NotNull(message = "页码不能为空") @Min(value = 1, message = "页码不能小于 1") @Schema(description = "页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer page = 1;

    @NotNull(message = "每页数量不能为空") @Min(value = 1, message = "每页数量不能小于 1") @Schema(description = "每页数量", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer size = 10;
}
