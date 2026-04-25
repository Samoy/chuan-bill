package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.dto.BillMonthlyStatsDTO;
import com.samoy.chuanbillserver.dto.StatisticsCategoryDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IStatisticsService;
import com.samoy.chuanbillserver.vo.BillMonthlyStatsVO;
import com.samoy.chuanbillserver.vo.CategoryStatisticsVO;
import com.samoy.chuanbillserver.vo.DailyTrendVO;
import com.samoy.chuanbillserver.vo.FamilyMemberStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 统计分析控制器
 * </p>
 *
 * @author samoy
 * @since 2026/4/11
 */
@Tag(name = "statistics", description = "统计分析相关接口")
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Resource
    private IStatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "获取月度统计概览", description = "获取指定月份的收入、支出、结余概览")
    public Result<BillMonthlyStatsVO> getOverview(@Validated @ModelAttribute BillMonthlyStatsDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(statisticsService.getOverview(userId, dto));
    }

    @GetMapping("/category")
    @Operation(summary = "获取分类统计", description = "获取指定月份按分类的收支统计，支持收入/支出切换")
    public Result<List<CategoryStatisticsVO>> getCategoryStats(@Validated @ModelAttribute StatisticsCategoryDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(statisticsService.getCategoryStats(userId, dto));
    }

    @GetMapping("/daily-trend")
    @Operation(summary = "获取每日收支趋势", description = "获取指定月份每日的收入和支出数据，用于折线图展示")
    public Result<List<DailyTrendVO>> getDailyTrend(@Validated @ModelAttribute BillMonthlyStatsDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(statisticsService.getDailyTrend(userId, dto));
    }

    @GetMapping("/members-bill")
    @Operation(summary = "获取成员账单统计", description = "获取指定月份的成员收支统计")
    public Result<List<FamilyMemberStatsVO>> getMembersStats(@Validated @ModelAttribute BillMonthlyStatsDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(statisticsService.getMembersStats(userId, dto));
    }
}
