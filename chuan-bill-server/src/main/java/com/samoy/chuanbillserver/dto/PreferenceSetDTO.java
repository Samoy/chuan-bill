package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "设置偏好请求")
public class PreferenceSetDTO {

    @NotBlank(message = "偏好键名不能为空") @Schema(
            description = "偏好键名",
            example = "notification.billReminder.enabled",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String key;

    @NotBlank(message = "偏好值不能为空") @Schema(description = "偏好值", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;
}
