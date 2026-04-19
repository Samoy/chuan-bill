package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "处理加入申请请求")
public class HandleJoinApplyDTO {

    @NotBlank(message = "申请ID不能为空") @Schema(description = "申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String applyId;

    @NotNull(message = "审批结果不能为空") @Schema(description = "是否同意", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean approved;
}
