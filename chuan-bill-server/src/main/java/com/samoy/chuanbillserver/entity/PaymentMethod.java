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
 * 支付方式表
 * </p>
 *
 * @author Samoy
 * @since 2026-04-22
 */
@Getter
@Setter
@ToString
@TableName("t_payment_method")
public class PaymentMethod implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付方式ID
     */
    @TableId("id")
    private String id;

    /**
     * 支付方式名称
     */
    @TableField("name")
    private String name;

    /**
     * 支付方式图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 支付方式排序，越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否默认支付方式，0否，1是
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 用户ID, 为空表示系统预设支付方式
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
