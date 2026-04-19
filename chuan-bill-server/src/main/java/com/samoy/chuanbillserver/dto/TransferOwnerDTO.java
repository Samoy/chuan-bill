package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "转让户主请求")
public class TransferOwnerDTO {

    @NotBlank(message = "家庭ID不能为空") @Schema(description = "家庭ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String familyId;

    @NotBlank(message = "目标用户ID不能为空") @Schema(description = "新户主用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetUserId;
}
