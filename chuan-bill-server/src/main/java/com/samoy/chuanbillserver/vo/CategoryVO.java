package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分类信息")
public class CategoryVO {
    @Schema(description = "分类 ID", example = "123456")
    private String id;

    @Schema(description = "分类名称", example = "餐饮")
    private String name;

    @Schema(description = "图标 URL", example = "https://example.com/icon/food.png")
    private String icon;

    @Schema(description = "分类类型：income-收入，expense-支出", example = "expense")
    private String type;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "是否默认", example = "true")
    private Boolean isDefault;

    @Schema(description = "用户 ID", example = "123456")
    private String userId;
}
