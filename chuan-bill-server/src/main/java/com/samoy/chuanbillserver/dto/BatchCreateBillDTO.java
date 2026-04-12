package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "批量创建账单请求")
public class BatchCreateBillDTO {
    @Valid @NotEmpty(message = "账单列表不能为空") @Schema(description = "账单列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AddBillDTO> bills;
}
