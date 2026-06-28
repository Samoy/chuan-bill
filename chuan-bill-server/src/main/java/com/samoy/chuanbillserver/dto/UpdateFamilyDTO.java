package com.samoy.chuanbillserver.dto;

import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.validation.TextModeration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新家庭请求")
public class UpdateFamilyDTO {

    @NotBlank(message = "家庭ID不能为空") @Schema(description = "家庭ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Size(max = 20, message = "家庭名称长度不能超过20个字符") @TextModeration(scene = ModerationScene.FAMILY_NAME, message = "家庭名称包含违规内容，请重新输入")
    @Schema(description = "家庭名称", example = "我的小家")
    private String name;

    @Schema(description = "家庭图标")
    private String avatar;

    @Size(max = 200, message = "家庭描述长度不能超过200个字符") @TextModeration(scene = ModerationScene.DESCRIPTION, message = "家庭描述包含违规内容，请重新输入")
    @Schema(description = "家庭描述", example = "温馨的三口之家")
    private String description;
}
