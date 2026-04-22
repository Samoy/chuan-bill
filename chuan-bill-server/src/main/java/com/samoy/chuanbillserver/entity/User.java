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
 * 用户表
 * </p>
 *
 * @author Samoy
 * @since 2026-04-22
 */
@Getter
@Setter
@ToString
@TableName("t_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId("id")
    private String id;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 微信 openid
     */
    @TableField("openid")
    private String openid;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 性别，0未知，1男，2女
     */
    @TableField("gender")
    private Byte gender;

    /**
     * 状态，0禁用，1启用
     */
    @TableField("status")
    private Boolean status;

    /**
     * 是否是VIP，0否，1是
     */
    @TableField("is_vip")
    private Boolean isVip;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

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
