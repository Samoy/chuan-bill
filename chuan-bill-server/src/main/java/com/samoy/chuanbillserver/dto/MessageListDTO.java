package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "消息列表查询参数")
public class MessageListDTO {

    @Schema(description = "消息类型：system-系统消息，family-家庭消息，bill-账单消息，budget-预算消息")
    private String type;

    @Schema(description = "消息状态：0-未读，1-已读")
    private Integer status;

    @Min(value = 1, message = "页码不能小于1") @Schema(description = "页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer page = 1;

    @Min(value = 1, message = "每页数量不能小于1") @Schema(description = "每页数量", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer size = 10;
}
