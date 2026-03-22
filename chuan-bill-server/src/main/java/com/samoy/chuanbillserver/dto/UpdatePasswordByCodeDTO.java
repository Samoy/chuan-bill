package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "通过验证码更新密码请求")
public class UpdatePasswordByCodeDTO {
    @NotBlank(message = "手机号不能为空") @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") @Schema(description = "手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "验证码不能为空") @Schema(description = "验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 20, message = "密码长度在 6 到 20 个字符之间") @Schema(description = "新密码", example = "newpass123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
