package com.samoy.chuanbillserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordByOldDTO {

    @NotBlank(message = "用户ID不能为空") private String userId;

    @NotBlank(message = "旧密码不能为空") private String oldPassword;

    @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 20, message = "密码长度在6到20个字符之间") private String newPassword;
}
