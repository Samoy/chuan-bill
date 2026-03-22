package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "通过旧密码更新密码请求")
public class UpdatePasswordByOldDTO {

    @NotBlank(message = "用户 ID 不能为空") @Schema(description = "用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @NotBlank(message = "旧密码不能为空") @Schema(description = "旧密码", example = "oldpass123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 20, message = "密码长度在 6 到 20 个字符之间") @Schema(description = "新密码", example = "newpass123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
