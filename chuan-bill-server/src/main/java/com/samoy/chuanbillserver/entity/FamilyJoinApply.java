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
 * 家庭加入申请表
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Getter
@Setter
@ToString
@TableName("t_family_join_apply")
public class FamilyJoinApply implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 家庭加入申请ID
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
     * 申请备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 申请状态，0待处理，1同意，2拒绝
     */
    @TableField("status")
    private Boolean status;

    /**
     * 处理申请的用户ID
     */
    @TableField("handle_user_id")
    private String handleUserId;

    /**
     * 处理时间
     */
    @TableField("handle_time")
    private LocalDateTime handleTime;

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
