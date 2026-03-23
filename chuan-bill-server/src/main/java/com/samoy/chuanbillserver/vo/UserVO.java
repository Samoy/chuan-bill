package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "用户信息")
public class UserVO {
    @Schema(description = "用户 ID", example = "123456")
    private String id;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像 URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "性别：0-未知，1-男，2-女", example = "1")
    private Byte gender;

    @Schema(description = "状态：false-禁用，true-正常", example = "true")
    private Boolean status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后登录时间", example = "2024-01-01 12:00:00")
    private LocalDateTime lastLoginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2024-01-01 10:00:00")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "2024-01-01 12:00:00")
    private LocalDateTime updateTime;
}
