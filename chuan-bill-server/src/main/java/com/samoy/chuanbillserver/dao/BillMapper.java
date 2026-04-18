package com.samoy.chuanbillserver.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.vo.CategoryStatisticsVO;
import com.samoy.chuanbillserver.vo.DailyTrendVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
     * @param type    收入还是支出
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 金额
     */
    BigDecimal selectMonthlyStats(
            String userId, String familyId, String type, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按分类统计金额
     *
     * @param userId    用户id
     * @param familyId  家庭id
     * @param type      收入还是支出
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分类统计列表
     */
    List<CategoryStatisticsVO> selectCategoryStats(
            String userId, String familyId, String type, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按日统计收支趋势
     *
     * @param userId    用户id
     * @param familyId  家庭id
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 每日收支趋势列表
     */
    List<DailyTrendVO> selectDailyTrend(String userId, String familyId, LocalDateTime startTime, LocalDateTime endTime);
}
