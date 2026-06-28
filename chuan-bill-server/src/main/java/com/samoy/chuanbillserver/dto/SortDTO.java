package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "排序请求")
public class SortDTO {

    @NotEmpty(message = "排序列表不能为空") @Schema(description = "排序后的ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> ids;
}
