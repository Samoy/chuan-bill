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
 * 用户偏好设置表
 * </p>
 *
 * @author Samoy
 * @since 2026-05-12
 */
@Getter
@Setter
@ToString
@TableName("t_user_preference")
public class UserPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId("id")
    private String id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 偏好键
     */
    @TableField("pref_key")
    private String prefKey;

    /**
     * 偏好值
     */
    @TableField("pref_value")
    private String prefValue;

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
}
