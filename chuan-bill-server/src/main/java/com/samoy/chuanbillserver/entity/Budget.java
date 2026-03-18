package com.samoy.chuanbillserver.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 预算表
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Getter
@Setter
@ToString
@TableName("t_budget")
public class Budget implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预算ID
     */
    @TableId("id")
    private String id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 家庭ID, 共享预算时填写
     */
    @TableField("family_id")
    private String familyId;

    /**
     * 预算月份(储存为当月第一天)
     */
    @TableField("month")
    private LocalDate month;

    /**
     * 预算金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 已使用金额
     */
    @TableField("use_amount")
    private BigDecimal useAmount;

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
