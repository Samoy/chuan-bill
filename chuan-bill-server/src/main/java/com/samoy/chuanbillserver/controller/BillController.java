package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.samoy.chuanbillserver.dto.AddBillDTO;
import com.samoy.chuanbillserver.dto.BatchCreateBillDTO;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.BillMonthlyStatsDTO;
import com.samoy.chuanbillserver.dto.UpdateBillDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.service.ICategoryService;
import com.samoy.chuanbillserver.service.IPaymentMethodService;
import com.samoy.chuanbillserver.vo.BillMonthlyStatsVO;
import com.samoy.chuanbillserver.vo.BillVO;
import com.samoy.chuanbillserver.vo.CategoryVO;
import com.samoy.chuanbillserver.vo.PaymentMethodVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bill")
@Tag(name = "bill", description = "账单的增删改查、分类、支付方式等相关接口")
public class BillController {

    @Resource
    private IBillService billService;

    @Resource
    private ICategoryService categoryService;

    @Resource
    private IPaymentMethodService paymentMethodService;

    @GetMapping("/page-list")
    @Operation(summary = "获取账单列表", description = "分页获取账单列表，支持多种筛选条件")
    public Result<IPage<BillVO>> getPageBillList(@Validated @ModelAttribute BillListDTO billListDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.getBillListByPage(userId, billListDTO));
    }

    @GetMapping("/detail")
    @Operation(summary = "获取账单详情", description = "根据 ID 获取账单详细信息")
    public Result<BillVO> getBillDetail(
            @Parameter(description = "账单 ID", required = true, example = "123456") @RequestParam String id) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.getBillDetail(userId, id));
    }

    @PostMapping("/add")
    @Operation(summary = "添加账单", description = "创建新的账单记录")
    public Result<Boolean> addBill(@Validated @RequestBody AddBillDTO addBillDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.addBill(userId, addBillDTO));
    }

    @PostMapping("/batchCreate")
    @Operation(summary = "批量添加账单", description = "批量创建账单记录，用于数据同步")
    public Result<Integer> batchCreate(@Validated @RequestBody BatchCreateBillDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.batchCreate(userId, dto));
    }

    @PostMapping("/update")
    @Operation(summary = "更新账单", description = "更新已有账单信息")
    public Result<Boolean> updateBill(@Validated @RequestBody UpdateBillDTO updateBillDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.updateBill(userId, updateBillDTO));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除账单", description = "根据 ID 删除账单记录")
    public Result<Boolean> deleteBill(
            @Parameter(description = "账单 ID", required = true, example = "123456") @RequestParam String id) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.deleteBill(userId, id));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取分类列表", description = "获取收入或支出的分类列表")
    public Result<List<CategoryVO>> getCategories(
            @Parameter(description = "分类类型：income-收入，expense-支出", example = "expense") @RequestParam(required = false)
                    String type) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(categoryService.getCategoryList(userId, type));
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "获取支付方式列表", description = "获取用户可用的支付方式列表")
    public Result<List<PaymentMethodVO>> getPaymentMethods() {
        String userId = StpUtil.getLoginIdAsString();
        List<PaymentMethodVO> result = paymentMethodService.getPaymentMethods(userId);
        return Result.success(result);
    }

    @GetMapping("/monthly-stats")
    @Operation(summary = "获取月度统计", description = "获取月度统计信息")
    public Result<BillMonthlyStatsVO> getMonthlyStats(
            @Validated @ModelAttribute BillMonthlyStatsDTO billMonthlyStatsDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.getMonthlyStats(userId, billMonthlyStatsDTO));
    }
}
