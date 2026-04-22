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
 * AI分析建议表
 * </p>
 *
 * @author Samoy
 * @since 2026-04-22
 */
@Getter
@Setter
@ToString
@TableName("t_ai_suggestion")
public class AiSuggestion implements Serializable {

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
     * 月份，格式YYYY-MM
     */
    @TableField("month")
    private String month;

    /**
     * 分析类型：1-个人，2-家庭
     */
    @TableField("analysis_type")
    private Integer analysisType;

    /**
     * 目标ID：个人为user_id，家庭为family_id
     */
    @TableField("target_id")
    private String targetId;

    /**
     * AI分析内容
     */
    @TableField("content")
    private String content;

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
