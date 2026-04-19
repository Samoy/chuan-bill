package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "家庭成员信息")
public class FamilyMemberVO {

    @Schema(description = "成员记录ID")
    private String id;

    @Schema(description = "家庭ID")
    private String familyId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "家庭内昵称")
    private String nickname;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "是否户主")
    private Boolean isOwner;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "加入时间")
    private LocalDateTime joinTime;
}
