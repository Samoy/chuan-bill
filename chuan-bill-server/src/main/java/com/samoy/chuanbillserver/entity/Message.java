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
 * 消息表
 * </p>
 *
 * @author Samoy
 * @since 2026-04-18
 */
@Getter
@Setter
@ToString
@TableName("t_message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId("id")
    private String id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 消息标题
     */
    @TableField("title")
    private String title;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 消息类型，system-系统消息，family-家庭相关消息，bill-账单相关消息，budget-预算相关消息
     */
    @TableField("type")
    private String type;

    /**
     * 消息状态，0未读，1已读
     */
    @TableField("status")
    private Boolean status;

    /**
     * 相关ID，根据消息类型不同而不同(如家庭id，账单id，预算id等)
     */
    @TableField("related_id")
    private String relatedId;

    /**
     * 相关类型，根据消息类型不同而不同: family-家庭，bill-账单，budget-预算
     */
    @TableField("related_type")
    private String relatedType;

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
