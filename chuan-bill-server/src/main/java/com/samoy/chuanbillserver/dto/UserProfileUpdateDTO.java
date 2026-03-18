package com.samoy.chuanbillserver.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateDTO {
    private String userId;

    @Size(max = 50, message = "昵称长度不能超过50个字符") private String nickname;

    private String avatar;

    @Pattern(regexp = "^[0-2]$", message = "性别必须是0、1或2") private Byte gender;
}
