package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "令牌响应")
public class TokenVO {
    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "过期时间（毫秒）", example = "86400000")
    private Long expireTime;

    @Schema(description = "用户 ID", example = "123456")
    private String userId;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;
}
