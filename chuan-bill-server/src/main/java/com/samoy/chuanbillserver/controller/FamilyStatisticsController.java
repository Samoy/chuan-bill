package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.dto.FamilyMemberStatsDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IFamilyStatisticsService;
import com.samoy.chuanbillserver.vo.FamilyAiSuggestionVO;
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
 * 家庭统计控制器
 * </p>
 *
 * @author samoy
 * @since 2026/4/21
 */
@RestController
@RequestMapping("/family/statistics")
@Tag(name = "family-statistics", description = "家庭统计相关接口")
public class FamilyStatisticsController {

    @Resource
    private IFamilyStatisticsService familyStatisticsService;

    @GetMapping("/members")
    @Operation(summary = "获取家庭成员收支统计", description = "获取指定月份各成员的收支统计及占比")
    public Result<List<FamilyMemberStatsVO>> getMemberStats(@Validated @ModelAttribute FamilyMemberStatsDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyStatisticsService.getMemberStats(userId, dto));
    }

    @GetMapping("/ai-suggestion")
    @Operation(summary = "获取家庭AI建议", description = "仅户主可用，每日限5次（多家庭共享额度）")
    public Result<FamilyAiSuggestionVO> getAiSuggestion(@Validated @ModelAttribute FamilyMemberStatsDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyStatisticsService.getAiSuggestion(userId, dto));
    }
}
