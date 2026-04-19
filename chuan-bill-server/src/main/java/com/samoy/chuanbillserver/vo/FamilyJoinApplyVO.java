package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "家庭加入申请信息")
public class FamilyJoinApplyVO {

    @Schema(description = "申请ID")
    private String id;

    @Schema(description = "家庭ID")
    private String familyId;

    @Schema(description = "家庭名称")
    private String familyName;

    @Schema(description = "申请人用户ID")
    private String userId;

    @Schema(description = "申请人昵称")
    private String userNickname;

    @Schema(description = "申请人头像")
    private String userAvatar;

    @Schema(description = "申请备注")
    private String remark;

    @Schema(description = "申请状态：0待处理，1同意，2拒绝")
    private Integer status;

    @Schema(description = "处理人用户ID")
    private String handleUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
