package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "微信登录请求")
public class LoginByWechatDTO {
    @NotBlank(message = "微信登录 code 不能为空") @Schema(description = "微信登录 code", example = "001abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
