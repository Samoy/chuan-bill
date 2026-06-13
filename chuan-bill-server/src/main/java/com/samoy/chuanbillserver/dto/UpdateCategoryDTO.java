package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新类目请求")
public class UpdateCategoryDTO {

    @Schema(description = "类目名称", example = "餐饮")
    private String name;

    @Schema(description = "类目图标", example = "icon-food")
    private String icon;
}
