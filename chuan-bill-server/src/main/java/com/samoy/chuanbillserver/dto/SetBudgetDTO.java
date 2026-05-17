package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Schema(description = "设置预算请求")
public class SetBudgetDTO {

    @NotNull(message = "预算金额不能为空") @DecimalMin(value = "0.01", message = "预算金额必须大于0") @Digits(integer = 10, fraction = 2) @Schema(description = "预算金额")
    private BigDecimal amount;
}
