package com.samoy.chuanbillserver.dto;

import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.validation.TextModeration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "添加支付方式请求")
public class AddPaymentMethodDTO {

    @NotBlank(message = "名称不能为空") @Size(max = 8, message = "名称最多8个字符") @Schema(description = "支付方式名称", example = "微信支付", requiredMode = Schema.RequiredMode.REQUIRED)
    @TextModeration(scene = ModerationScene.TITLE, message = "名称包含违规内容，请重新输入")
    private String name;

    @NotBlank(message = "支付方式图标不能为空") @Schema(description = "支付方式图标", example = "icon-wechat", requiredMode = Schema.RequiredMode.REQUIRED)
    private String icon;
}
