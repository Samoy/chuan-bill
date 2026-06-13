package com.samoy.chuanbillserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCategoryDTO {

    @NotBlank(message = "类目名称不能为空") private String name;

    @NotBlank(message = "类目图标不能为空") private String icon;

    @NotBlank(message = "类目类型不能为空") private String type;
}
