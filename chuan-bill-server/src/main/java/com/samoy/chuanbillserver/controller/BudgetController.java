package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.dto.SetBudgetDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IBudgetService;
import com.samoy.chuanbillserver.vo.BudgetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/budget")
@Tag(name = "budget", description = "预算管理相关接口")
public class BudgetController {

    @Resource
    private IBudgetService budgetService;

    @GetMapping("/current")
    @Operation(summary = "获取当月预算", description = "获取指定月份的预算信息，含实时计算的已用金额")
    public Result<BudgetVO> getCurrentBudget(
            @Parameter(description = "月份，格式 YYYY-MM，默认当月") @RequestParam(required = false) String month) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(budgetService.getCurrentBudget(userId, month));
    }

    @PostMapping("/set")
    @Operation(summary = "设置预算", description = "设置或修改当月预算金额")
    public Result<BudgetVO> setBudget(@Validated @RequestBody SetBudgetDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(budgetService.setBudget(userId, dto));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除预算", description = "删除当月预算")
    public Result<Boolean> deleteBudget() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(budgetService.deleteBudget(userId));
    }
}
