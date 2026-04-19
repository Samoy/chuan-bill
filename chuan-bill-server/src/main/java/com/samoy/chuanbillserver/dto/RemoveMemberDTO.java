package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "移除家庭成员请求")
public class RemoveMemberDTO {

    @NotBlank(message = "家庭ID不能为空") @Schema(description = "家庭ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String familyId;

    @NotBlank(message = "成员ID不能为空") @Schema(description = "家庭成员记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String memberId;
}
