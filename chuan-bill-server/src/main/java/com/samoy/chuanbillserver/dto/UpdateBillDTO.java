package com.samoy.chuanbillserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UpdateBillDTO {
    @NotBlank(message = "账单ID不能为空") private String id;

    @Size(min = 1, max = 50, message = "账单名称长度在1到50个字符之间") private String name;

    private String categoryId;

    private String paymentMethodId;

    @Pattern(regexp = "^(income|expense)$", message = "账单类型必须是income或expense")
    private String type;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime time;

    @DecimalMin(value = "0.01", message = "账单金额必须大于0") @Digits(integer = 10, fraction = 2, message = "账单金额最多10位数字，且小数点后最多2位") private BigDecimal amount;

    @Size(max = 500, message = "账单备注长度不能超过500个字符") private String remark;
}
