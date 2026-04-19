package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "家庭信息")
public class FamilyVO {

    @Schema(description = "家庭ID")
    private String id;

    @Schema(description = "家庭名称", example = "我的小家")
    private String name;

    @Schema(description = "家庭图标")
    private String avatar;

    @Schema(description = "家庭描述")
    private String description;

    @Schema(description = "户主ID")
    private String ownerId;

    @Schema(description = "户主昵称")
    private String ownerNickname;

    @Schema(description = "户主头像")
    private String ownerAvatar;

    @Schema(description = "邀请码")
    private String inviteCode;

    @Schema(description = "成员数量")
    private Integer memberCount;

    @Schema(description = "当前用户是否户主")
    private Boolean isOwner;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
