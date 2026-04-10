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

    @Pattern(regexp = "^(income|expense|)$", message = "类型不正确")
    @Schema(description = "账单类型：income-收入，expense-支出，空字符串：全部", example = "expense")
    private String type;

    @Schema(description = "支付方式 ID", example = "123456")
    private String paymentMethodId;

    @DecimalMin(value = "0.00", message = "最小金额不能小于 0.00", inclusive = false) @Digits(integer = 10, fraction = 2, message = "最小金额格式错误，最多 10 位整数，2 位小数") @Schema(description = "最小金额", example = "10.00")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.00", message = "最大金额不能小于 0.00", inclusive = false) @Digits(integer = 10, fraction = 2, message = "最大金额格式错误，最多 10 位整数，2 位小数") @Schema(description = "最大金额", example = "1000.00")
    private BigDecimal maxAmount;

    @Size(max = 50, message = "关键字长度不能超过50个字符") @Schema(description = "关键字模糊搜索，支持名称和备注", example = "早餐")
    private String keyword;

    @Min(value = 1, message = "页码不能小于 1") @Schema(description = "页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer page = 1;

    @Min(value = 1, message = "每页数量不能小于 1") @Schema(description = "每页数量", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer size = 10;
}
