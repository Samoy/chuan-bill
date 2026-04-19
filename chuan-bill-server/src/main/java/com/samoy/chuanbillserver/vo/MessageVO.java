package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "消息信息")
public class MessageVO {

    @Schema(description = "消息ID")
    private String id;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型：system-系统消息，family-家庭消息，bill-账单消息，budget-预算消息")
    private String type;

    @Schema(description = "消息状态：0未读，1已读")
    private Integer status;

    @Schema(description = "相关ID")
    private String relatedId;

    @Schema(description = "相关类型：family-家庭，bill-账单，budget-预算")
    private String relatedType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
