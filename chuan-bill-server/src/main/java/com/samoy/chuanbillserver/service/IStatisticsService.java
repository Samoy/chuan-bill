package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.dto.BillMonthlyStatsDTO;
import com.samoy.chuanbillserver.dto.StatisticsCategoryDTO;
import com.samoy.chuanbillserver.vo.BillMonthlyStatsVO;
import com.samoy.chuanbillserver.vo.CategoryStatisticsVO;
import com.samoy.chuanbillserver.vo.DailyTrendVO;
import java.util.List;

/**
 * <p>
 * 统计分析服务接口
 * </p>
 *
 * @author samoy
 * @since 2026/4/17
 */
public interface IStatisticsService {

    /**
     * 获取月度统计概览
     *
     * @param userId 用户id
     * @param dto    请求参数
     * @return 月度统计信息
     */
    BillMonthlyStatsVO getOverview(String userId, BillMonthlyStatsDTO dto);

    /**
     * 获取分类统计
     *
     * @param userId 用户id
     * @param dto    请求参数
     * @return 分类统计列表
     */
    List<CategoryStatisticsVO> getCategoryStats(String userId, StatisticsCategoryDTO dto);

    /**
     * 获取每日收支趋势
     *
     * @param userId 用户id
     * @param dto    请求参数
     * @return 每日收支趋势列表
     */
    List<DailyTrendVO> getDailyTrend(String userId, BillMonthlyStatsDTO dto);
}
