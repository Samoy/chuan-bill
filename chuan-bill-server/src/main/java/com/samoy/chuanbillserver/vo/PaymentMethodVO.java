package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付方式信息")
public class PaymentMethodVO {
    @Schema(description = "支付方式 ID", example = "123456")
    private String id;

    @Schema(description = "支付方式名称", example = "微信支付")
    private String name;

    @Schema(description = "图标 URL", example = "https://example.com/icon/wechat.png")
    private String icon;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "是否默认", example = "true")
    private Boolean isDefault;

    @Schema(description = "用户 ID", example = "123456")
    private String userId;
}
