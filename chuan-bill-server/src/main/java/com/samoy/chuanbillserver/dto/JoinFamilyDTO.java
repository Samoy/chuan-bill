package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "申请加入家庭请求")
public class JoinFamilyDTO {

    @NotBlank(message = "邀请码不能为空") @Schema(description = "家庭邀请码", example = "AB12CD34", requiredMode = Schema.RequiredMode.REQUIRED)
    private String inviteCode;

    @Size(max = 200, message = "申请备注长度不能超过200个字符") @Schema(description = "申请备注", example = "我是家庭成员，请通过")
    private String remark;
}
