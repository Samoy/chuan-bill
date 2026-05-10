package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "通过验证码更新手机号参数")
public class UpdatePhoneByCodeDTO {
    @NotBlank(message = "旧手机号验证码不能为空") @Schema(description = "旧手机号验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPhoneCode;

    @NotBlank(message = "新手机号不能为空") @Pattern(regexp = "^1[3-9]\\d{8}$", message = "新手机号格式错误") @Schema(description = "新手机号", example = "13012345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPhone;

    @NotBlank(message = "新手机号验证码不能为空") @Schema(description = "新手机号验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPhoneCode;
}
