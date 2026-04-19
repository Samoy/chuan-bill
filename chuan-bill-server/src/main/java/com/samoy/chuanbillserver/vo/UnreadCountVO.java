package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "未读消息数量")
public class UnreadCountVO {

    @Schema(description = "总未读数")
    private Integer total;

    @Schema(description = "家庭相关未读数")
    private Integer familyCount;
}
