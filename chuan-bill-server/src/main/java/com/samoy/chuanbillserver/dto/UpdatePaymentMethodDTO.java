package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新支付方式请求")
public class UpdatePaymentMethodDTO {

    @Schema(description = "支付方式名称", example = "微信支付")
    private String name;

    @Schema(description = "支付方式图标", example = "icon-wechat")
    private String icon;
}
