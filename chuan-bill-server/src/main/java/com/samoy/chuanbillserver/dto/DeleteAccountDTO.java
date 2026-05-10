package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "注销账号请求参数")
public class DeleteAccountDTO {

    @NotBlank(message = "验证码不能为空") @Schema(description = "手机验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
