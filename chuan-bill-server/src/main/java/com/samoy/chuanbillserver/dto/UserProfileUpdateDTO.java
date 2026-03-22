package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户资料更新请求")
public class UserProfileUpdateDTO {
    @Schema(description = "用户 ID")
    private String userId;

    @Size(max = 50, message = "昵称长度不能超过 50 个字符") @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像 URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Pattern(regexp = "^[0-2]$", message = "性别必须是 0、1 或 2") @Schema(description = "性别：0-未知，1-男，2-女", example = "1")
    private Byte gender;
}
