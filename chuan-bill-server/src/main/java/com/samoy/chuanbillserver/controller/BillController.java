package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.samoy.chuanbillserver.annotation.Idempotent;
import com.samoy.chuanbillserver.dto.AddBillDTO;
import com.samoy.chuanbillserver.dto.AddCategoryDTO;
import com.samoy.chuanbillserver.dto.AddPaymentMethodDTO;
import com.samoy.chuanbillserver.dto.BatchCreateBillDTO;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.BillMonthlyStatsDTO;
import com.samoy.chuanbillserver.dto.ExportBillDTO;
import com.samoy.chuanbillserver.dto.SortDTO;
import com.samoy.chuanbillserver.dto.UpdateBillDTO;
import com.samoy.chuanbillserver.dto.UpdateCategoryDTO;
import com.samoy.chuanbillserver.dto.UpdatePaymentMethodDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.service.ICategoryService;
import com.samoy.chuanbillserver.service.IPaymentMethodService;
import com.samoy.chuanbillserver.vo.BatchSyncResultVO;
import com.samoy.chuanbillserver.vo.BillMonthlyStatsVO;
import com.samoy.chuanbillserver.vo.BillVO;
import com.samoy.chuanbillserver.vo.CategoryVO;
import com.samoy.chuanbillserver.vo.PaymentMethodVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
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

    @Idempotent
    @PostMapping("/add")
    @Operation(summary = "添加账单", description = "创建新的账单记录")
    public Result<Boolean> addBill(@Validated @RequestBody AddBillDTO addBillDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.addBill(userId, addBillDTO));
    }

    @Idempotent
    @PostMapping("/batchCreate")
    @Operation(summary = "批量添加账单", description = "批量创建账单记录，用于数据同步")
    public Result<BatchSyncResultVO> batchCreate(@Validated @RequestBody BatchCreateBillDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.batchCreate(userId, dto));
    }

    @Idempotent
    @PostMapping("/update")
    @Operation(summary = "更新账单", description = "更新已有账单信息")
    public Result<Boolean> updateBill(@Validated @RequestBody UpdateBillDTO updateBillDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.updateBill(userId, updateBillDTO));
    }

    @Idempotent
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

    @Idempotent
    @PostMapping("/categories")
    @Operation(summary = "新增自定义类目", description = "用户新增自定义类目")
    public Result<CategoryVO> addCategory(@Validated @RequestBody AddCategoryDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(categoryService.addCategory(userId, dto));
    }

    @Idempotent
    @PutMapping("/categories/{id}")
    @Operation(summary = "更新自定义类目", description = "用户更新自定义类目名称和图标")
    public Result<CategoryVO> updateCategory(
            @Parameter(description = "类目 ID", required = true) @PathVariable String id,
            @Validated @RequestBody UpdateCategoryDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(categoryService.updateCategory(userId, id, dto));
    }

    @Idempotent
    @DeleteMapping("/categories/{id}")
    @Operation(summary = "删除自定义类目", description = "用户删除自定义类目，有关联账单时不可删除")
    public Result<Boolean> deleteCategory(@Parameter(description = "类目 ID", required = true) @PathVariable String id) {
        String userId = StpUtil.getLoginIdAsString();
        categoryService.deleteCategory(userId, id);
        return Result.success(true);
    }

    @Idempotent
    @PutMapping("/categories/sort")
    @Operation(summary = "批量更新类目排序", description = "用户批量更新自定义类目的排序")
    public Result<Boolean> sortCategories(@Validated @RequestBody SortDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        categoryService.sortCategories(userId, dto.getIds());
        return Result.success(true);
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "获取支付方式列表", description = "获取用户可用的支付方式列表")
    public Result<List<PaymentMethodVO>> getPaymentMethods() {
        String userId = StpUtil.getLoginIdAsString();
        List<PaymentMethodVO> result = paymentMethodService.getPaymentMethods(userId);
        return Result.success(result);
    }

    @Idempotent
    @PostMapping("/payment-methods")
    @Operation(summary = "新增自定义支付方式", description = "用户新增自定义支付方式")
    public Result<PaymentMethodVO> addPaymentMethod(@Validated @RequestBody AddPaymentMethodDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(paymentMethodService.addPaymentMethod(userId, dto));
    }

    @Idempotent
    @PutMapping("/payment-methods/{id}")
    @Operation(summary = "更新自定义支付方式", description = "用户更新自定义支付方式名称和图标")
    public Result<PaymentMethodVO> updatePaymentMethod(
            @Parameter(description = "支付方式 ID", required = true) @PathVariable String id,
            @Validated @RequestBody UpdatePaymentMethodDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(paymentMethodService.updatePaymentMethod(userId, id, dto));
    }

    @Idempotent
    @DeleteMapping("/payment-methods/{id}")
    @Operation(summary = "删除自定义支付方式", description = "用户删除自定义支付方式，有关联账单时不可删除")
    public Result<Boolean> deletePaymentMethod(
            @Parameter(description = "支付方式 ID", required = true) @PathVariable String id) {
        String userId = StpUtil.getLoginIdAsString();
        paymentMethodService.deletePaymentMethod(userId, id);
        return Result.success(true);
    }

    @Idempotent
    @PutMapping("/payment-methods/sort")
    @Operation(summary = "批量更新支付方式排序", description = "用户批量更新自定义支付方式的排序")
    public Result<Boolean> sortPaymentMethods(@Validated @RequestBody SortDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        paymentMethodService.sortPaymentMethods(userId, dto.getIds());
        return Result.success(true);
    }

    @GetMapping("/monthly-stats")
    @Operation(summary = "获取月度统计", description = "获取月度统计信息")
    public Result<BillMonthlyStatsVO> getMonthlyStats(
            @Validated @ModelAttribute BillMonthlyStatsDTO billMonthlyStatsDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.getMonthlyStats(userId, billMonthlyStatsDTO));
    }

    @Idempotent
    @PostMapping("/export")
    @Operation(summary = "导出账单", description = "根据筛选条件导出账单，支持 Excel 和 PDF 格式")
    public void exportBill(@Validated @RequestBody ExportBillDTO dto, HttpServletResponse response) {
        String userId = StpUtil.getLoginIdAsString();
        billService.exportBill(userId, dto, response);
    }
}
