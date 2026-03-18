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
 * 家庭表
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Getter
@Setter
@ToString
@TableName("t_family")
public class Family implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 家庭ID
     */
    @TableId("id")
    private String id;

    /**
     * 家庭名称
     */
    @TableField("name")
    private String name;

    /**
     * 家庭图标
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 家庭户主ID
     */
    @TableField("owner_id")
    private String ownerId;

    /**
     * 邀请码，用于加入家庭
     */
    @TableField("invite_code")
    private String inviteCode;

    /**
     * 家庭描述
     */
    @TableField("description")
    private String description;

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
