package com.samoy.chuanbillserver.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samoy.chuanbillserver.entity.Bill;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 账单表 Mapper 接口
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
public interface BillMapper extends BaseMapper<Bill> {
    /**
     * 查询月统计
     *
     * @param userId    用户id
     * @param familyId  家庭id
     * @param income    收入还是支出
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 金额
     */
    BigDecimal selectMonthlyStats(
            String userId, String familyId, String income, LocalDateTime startTime, LocalDateTime endTime);
}
