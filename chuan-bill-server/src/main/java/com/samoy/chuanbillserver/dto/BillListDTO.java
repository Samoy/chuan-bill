package com.samoy.chuanbillserver.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class BillListDTO {
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式错误") private String startDate;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式错误") private String endDate;

    private String categoryId;

    @Pattern(regexp = "^(income|expense)$", message = "类型只能是income或expense")
    private String type;

    @DecimalMin(value = "0.00", message = "最小金额不能小于0.00", inclusive = false) @Digits(integer = 10, fraction = 2, message = "最小金额格式错误, 最多10位整数, 2位小数") private BigDecimal minAmount;

    @DecimalMin(value = "0.00", message = "最大金额不能小于0.00", inclusive = false) @Digits(integer = 10, fraction = 2, message = "最大金额格式错误, 最多10位整数, 2位小数") private BigDecimal maxAmount;

    @Size(max = 50, message = "账单名称长度不能超过50个字符") private String name;

    @Size(max = 500, message = "账单备注长度不能超过500个字符") private String remark;

    @NotNull(message = "页码不能为空") @Min(value = 1, message = "页码不能小于1") private Integer page = 1;

    @NotNull(message = "每页数量不能为空") @Min(value = 1, message = "每页数量不能小于1") private Integer size = 10;
}
