package com.samoy.chuanbillserver.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 家庭成员表
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Getter
@Setter
@ToString
@TableName("t_family_member")
public class FamilyMember implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 家庭成员ID
     */
    @TableId("id")
    private String id;

    /**
     * 家庭ID
     */
    @TableField("family_id")
    private String familyId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 家庭成员昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 是否是户主，0否，1是
     */
    @TableField("is_owner")
    private Boolean isOwner;

    /**
     * 加入时间
     */
    @TableField("join_time")
    private LocalDateTime joinTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 是否删除，0未删除，1已删除
     */
    @TableField("deleted")
    private Boolean deleted;
}
