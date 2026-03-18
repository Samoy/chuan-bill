package com.samoy.chuanbillserver.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 账单表
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Getter
@Setter
@ToString
@TableName("t_bill")
public class Bill implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账单ID
     */
    @TableId("id")
    private String id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 家庭ID, 共享账单时填写
     */
    @TableField("family_id")
    private String familyId;

    /**
     * 账单名称
     */
    @TableField("name")
    private String name;

    /**
     * 类目ID
     */
    @TableField("category_id")
    private String categoryId;

    /**
     * 支付方式ID
     */
    @TableField("payment_method_id")
    private String paymentMethodId;

    /**
     * 账单类型，income-收入，expense-支出
     */
    @TableField("type")
    private String type;

    /**
     * 金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 账单日期, 默认当前时间
     */
    @TableField("time")
    private LocalDateTime time;

    /**
     * 账单备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 账单来源，manual-手动添加，ocr-OCR识别, voice-语音输入，import-导入
     */
    @TableField("source")
    private String source;

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
