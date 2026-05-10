package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BindPhoneDTO {
    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空") @Pattern(regexp = "^1[3-9]\\d{8}$", message = "手机号格式错误") @Schema(description = "手机号", example = "13888888888", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;
    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空") @Schema(description = "验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
