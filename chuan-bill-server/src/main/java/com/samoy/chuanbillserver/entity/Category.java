package com.samoy.chuanbillserver.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 类目表
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Getter
@Setter
@ToString
@TableName("t_category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类目ID
     */
    @TableId("id")
    private String id;

    /**
     * 类目名称
     */
    @TableField("name")
    private String name;

    /**
     * 类目图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 类目类型，income-收入，expense-支出
     */
    @TableField("type")
    private String type;

    /**
     * 类目排序，越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否默认类目，0否，1是
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 用户ID, 为空表示系统预设类目
     */
    @TableField("user_id")
    private String userId;

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
