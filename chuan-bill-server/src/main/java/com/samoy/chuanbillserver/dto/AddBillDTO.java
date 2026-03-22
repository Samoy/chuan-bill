package com.samoy.chuanbillserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "添加账单请求")
public class AddBillDTO {

    @NotBlank(message = "账单名称不能为空") @Size(min = 1, max = 50, message = "账单名称长度在 1 到 50 个字符之间") @Schema(description = "账单名称", example = "早餐", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "账单类目 id 不能为空") @Schema(description = "分类 ID", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryId;

    @Schema(description = "支付方式 ID", example = "123456")
    private String paymentMethodId;

    @NotBlank(message = "账单类型不能为空") @Pattern(regexp = "^(income|expense)$", message = "账单类型必须是 income 或 expense")
    @Schema(description = "账单类型：income-收入，expense-支出", example = "expense", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotNull(message = "账单金额不能为空") @DecimalMin(value = "0.01", message = "账单金额必须大于 0") @Digits(integer = 10, fraction = 2, message = "账单金额最多 10 位数字，且小数点后最多 2 位") @Schema(description = "账单金额", example = "10.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotNull(message = "账单时间不能为空") @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "账单时间", example = "2024-01-01 08:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime time;

    @Size(max = 500, message = "账单备注长度不能超过 500 个字符") @Schema(description = "账单备注", example = "在公司楼下吃的早餐")
    private String remark;

    @Schema(description = "家庭 ID（可选）", example = "family123")
    private String familyId;

    @Pattern(regexp = "^(manual|ocr|voice)$", message = "账单来源必须是 manual、ocr 或 voice")
    @Schema(description = "账单来源：manual-手动，ocr-图片识别，voice-语音", example = "manual")
    private String source;
}
