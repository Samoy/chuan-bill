package com.samoy.chuanbillserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "更新账单请求")
public class UpdateBillDTO {
    @NotBlank(message = "账单 ID 不能为空") @Schema(description = "账单 ID", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Size(min = 1, max = 50, message = "账单名称长度在 1 到 50 个字符之间") @Schema(description = "账单名称", example = "午餐")
    private String name;

    @Schema(description = "分类 ID", example = "123456")
    private String categoryId;

    @Schema(description = "支付方式 ID", example = "123456")
    private String paymentMethodId;

    @Pattern(regexp = "^(income|expense)$", message = "账单类型必须是 income 或 expense")
    @Schema(description = "账单类型：income-收入，expense-支出", example = "expense")
    private String type;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "账单时间", example = "2024-01-01 12:00")
    private LocalDateTime time;

    @DecimalMin(value = "0.01", message = "账单金额必须大于 0") @Digits(integer = 10, fraction = 2, message = "账单金额最多 10 位数字，且小数点后最多 2 位") @Schema(description = "账单金额", example = "25.00")
    private BigDecimal amount;

    @Size(max = 500, message = "账单备注长度不能超过 500 个字符") @Schema(description = "账单备注", example = "公司附近的餐厅")
    private String remark;
}
