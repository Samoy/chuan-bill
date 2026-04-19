package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "账单信息")
public class BillVO {
    @Schema(description = "账单 ID", example = "123456")
    private String id;

    @Schema(description = "账单名称", example = "早餐")
    private String name;

    @Schema(description = "分类信息")
    private CategoryVO category;

    @Schema(description = "支付方式信息")
    private PaymentMethodVO paymentMethod;

    @Schema(description = "账单类型：income-收入，expense-支出", example = "expense")
    private String type;

    @Schema(description = "账单金额", example = "10.50")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "账单时间", example = "2024-01-01 08:00")
    private LocalDateTime time;

    @Schema(description = "账单备注", example = "在公司楼下吃的早餐")
    private String remark;

    @Schema(description = "账单来源：manual-手动，ocr-图片识别，voice-语音", example = "manual")
    private String source;

    @Schema(description = "家庭 ID", example = "family123")
    private String familyId;

    @Schema(description = "家庭名称", example = "小川的家")
    private String familyName;
}
