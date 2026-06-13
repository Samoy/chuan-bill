package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "添加类目请求")
public class AddCategoryDTO {

    @NotBlank(message = "类目名称不能为空") @Size(max = 8, message = "名称最多8个字符") @Schema(description = "类目名称", example = "餐饮", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "类目图标不能为空") @Schema(description = "类目图标", example = "icon-food", requiredMode = Schema.RequiredMode.REQUIRED)
    private String icon;

    @NotBlank(message = "类目类型不能为空") @Pattern(regexp = "^(income|expense)$", message = "类型只能是 income 或 expense")
    @Schema(description = "类目类型：income-收入，expense-支出", example = "expense", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;
}
