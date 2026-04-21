package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * <p>
 * 家庭成员统计数据
 * </p>
 *
 * @author samoy
 * @since 2026/4/21
 */
@Data
@Schema(description = "家庭成员统计数据")
public class FamilyMemberStatsVO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "支出金额")
    private BigDecimal expense;

    @Schema(description = "收入金额")
    private BigDecimal income;

    @Schema(description = "支出占比(%)")
    private BigDecimal expensePercentage;

    @Schema(description = "收入占比(%)")
    private BigDecimal incomePercentage;

    @Schema(description = "是否是户主")
    private Boolean isOwner;
}
