package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.dto.AddBillDTO;
import com.samoy.chuanbillserver.dto.BatchCreateBillDTO;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.BillMonthlyStatsDTO;
import com.samoy.chuanbillserver.dto.UpdateBillDTO;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.entity.Category;
import com.samoy.chuanbillserver.entity.PaymentMethod;
import com.samoy.chuanbillserver.expection.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.service.ICategoryService;
import com.samoy.chuanbillserver.service.IPaymentMethodService;
import com.samoy.chuanbillserver.vo.BillMonthlyStatsVO;
import com.samoy.chuanbillserver.vo.BillVO;
import com.samoy.chuanbillserver.vo.CategoryVO;
import com.samoy.chuanbillserver.vo.PaymentMethodVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 账单表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements IBillService {

    @Resource
    private ICategoryService categoryService;

    @Resource
    private IPaymentMethodService paymentMethodService;

    @Override
    public List<BillVO> getBillList(String userId, BillListDTO billListDTO) {
        LambdaQueryWrapper<Bill> wrapper = buildQueryWrapper(userId, billListDTO);
        List<Bill> billList = baseMapper.selectList(wrapper);
        return convertToBillVOList(billList);
    }

    @Override
    public IPage<BillVO> getBillListByPage(String userId, BillListDTO billListDTO) {
        LambdaQueryWrapper<Bill> wrapper = buildQueryWrapper(userId, billListDTO);
        IPage<Bill> billPage = this.page(new Page<>(billListDTO.getPage(), billListDTO.getSize()), wrapper);

        return convertToBillVOPage(billPage);
    }

    @Override
    public boolean addBill(String userId, AddBillDTO addBillDTO) {
        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setName(addBillDTO.getName());
        bill.setCategoryId(addBillDTO.getCategoryId());
        bill.setPaymentMethodId(addBillDTO.getPaymentMethodId());
        bill.setType(addBillDTO.getType());
        bill.setAmount(addBillDTO.getAmount());
        bill.setTime(addBillDTO.getTime());
        bill.setRemark(addBillDTO.getRemark());
        bill.setSource(addBillDTO.getSource());
        if (addBillDTO.getFamilyId() != null) {
            bill.setFamilyId(addBillDTO.getFamilyId());
        }
        return this.save(bill);
    }

    @Override
    public int batchCreate(String userId, BatchCreateBillDTO dto) {
        List<AddBillDTO> bills = dto.getBills();
        if (bills == null || bills.isEmpty()) {
            return 0;
        }
        // 转换为 Bill 实体列表
        List<Bill> billList = bills.stream()
                .map(addBillDTO -> {
                    Bill bill = new Bill();
                    bill.setUserId(userId);
                    bill.setName(addBillDTO.getName());
                    bill.setCategoryId(addBillDTO.getCategoryId());
                    bill.setPaymentMethodId(addBillDTO.getPaymentMethodId());
                    bill.setType(addBillDTO.getType());
                    bill.setAmount(addBillDTO.getAmount());
                    bill.setTime(addBillDTO.getTime());
                    bill.setRemark(addBillDTO.getRemark());
                    bill.setSource(addBillDTO.getSource() != null ? addBillDTO.getSource() : "manual");
                    if (addBillDTO.getFamilyId() != null) {
                        bill.setFamilyId(addBillDTO.getFamilyId());
                    }
                    return bill;
                })
                .toList();
        // 使用 MyBatis-Plus 的 saveBatch 批量保存
        ((IBillService) AopContext.currentProxy()).saveBatch(billList);
        return billList.size();
    }

    @Override
    public boolean updateBill(String userId, UpdateBillDTO updateBillDTO) {
        Bill bill = this.getById(updateBillDTO.getId());
        if (bill == null) {
            throw new BusinessException(ResultEnum.BILL_NOT_FOUND);
        }
        // 权限校验: 只能更新自己的账单
        if (!Objects.equals(bill.getUserId(), userId)) {
            throw new BusinessException(ResultEnum.BILL_NOT_ALLOW_UPDATE);
        }
        bill.setName(updateBillDTO.getName());
        bill.setCategoryId(updateBillDTO.getCategoryId());
        bill.setPaymentMethodId(updateBillDTO.getPaymentMethodId());
        bill.setType(updateBillDTO.getType());
        bill.setAmount(updateBillDTO.getAmount());
        bill.setTime(updateBillDTO.getTime());
        bill.setRemark(updateBillDTO.getRemark());
        return this.updateById(bill);
    }

    @Override
    public boolean deleteBill(String userId, String billId) {
        Bill bill = this.getById(billId);
        if (bill == null) {
            throw new BusinessException(ResultEnum.BILL_NOT_FOUND);
        }
        if (!Objects.equals(bill.getUserId(), userId)) {
            throw new BusinessException(ResultEnum.BILL_NOT_ALLOW_DELETE);
        }
        return this.removeById(billId);
    }

    @Override
    public BillVO getBillDetail(String userId, String billId) {
        Bill bill = this.getById(billId);
        if (bill == null) {
            throw new BusinessException(ResultEnum.BILL_NOT_FOUND);
        }
        if (!Objects.equals(bill.getUserId(), userId)) {
            throw new BusinessException(ResultEnum.BILL_NOT_ALLOW_VIEW);
        }
        // 单个账单查询，直接使用简化版本
        return this.getBillVO(bill);
    }

    @Override
    public BillMonthlyStatsVO getMonthlyStats(String userId, BillMonthlyStatsDTO dto) {
        YearMonth yearMonth = YearMonth.parse(dto.getMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime startTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 查询收入总额
        BigDecimal income = this.baseMapper.selectMonthlyStats(userId, dto.getFamilyId(), "income", startTime, endTime);
        // 查询支出总额
        BigDecimal expense =
                this.baseMapper.selectMonthlyStats(userId, dto.getFamilyId(), "expense", startTime, endTime);

        // 处理null值
        income = income == null ? new BigDecimal("0.00") : income;
        expense = expense == null ? new BigDecimal("0.00") : expense;

        // 计算结余
        BigDecimal balance = income.subtract(expense);

        // 返回数据
        BillMonthlyStatsVO vo = new BillMonthlyStatsVO();
        vo.setMonth(dto.getMonth());
        vo.setIncome(income);
        vo.setExpense(expense);
        vo.setBalance(balance);
        return vo;
    }

    private LambdaQueryWrapper<Bill> buildQueryWrapper(String userId, BillListDTO billListDTO) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId).orderByDesc(Bill::getTime, Bill::getCreateTime);

        // 日期范围查询
        if (ObjectUtil.isNotEmpty(billListDTO.getStartDate())) {
            wrapper.ge(Bill::getTime, LocalDateTime.parse(String.format("%sT00:00:00", billListDTO.getStartDate())));
        }
        if (ObjectUtil.isNotEmpty(billListDTO.getEndDate())) {
            wrapper.le(Bill::getTime, LocalDateTime.parse(String.format("%sT23:59:59", billListDTO.getEndDate())));
        }

        // 类型过滤
        if (ObjectUtil.isNotEmpty(billListDTO.getType())) {
            wrapper.eq(Bill::getType, billListDTO.getType());
        }

        // 分类过滤
        if (ObjectUtil.isNotEmpty(billListDTO.getCategoryId())) {
            wrapper.eq(Bill::getCategoryId, billListDTO.getCategoryId());
        }

        // 支付方式过滤
        if (ObjectUtil.isNotEmpty(billListDTO.getPaymentMethodId())) {
            wrapper.eq(Bill::getPaymentMethodId, billListDTO.getPaymentMethodId());
        }

        // 金额范围过滤
        if (ObjectUtil.isNotEmpty(billListDTO.getMinAmount())) {
            wrapper.ge(Bill::getAmount, billListDTO.getMinAmount());
        }
        if (ObjectUtil.isNotEmpty(billListDTO.getMaxAmount())) {
            wrapper.le(Bill::getAmount, billListDTO.getMaxAmount());
        }
        // 关键字模糊查询
        if (ObjectUtil.isNotEmpty(billListDTO.getKeyword())) {
            String keyword = billListDTO.getKeyword();
            wrapper.like(Bill::getName, keyword).or().like(Bill::getRemark, keyword);
        }
        return wrapper;
    }

    /**
     * 转换单个 Bill 为 BillVO（适用于单个查询场景）
     */
    private BillVO getBillVO(Bill bill) {
        BillVO billVO = new BillVO();
        billVO.setId(bill.getId());
        billVO.setFamilyId(bill.getFamilyId());
        billVO.setName(bill.getName());

        // 单个查询：直接获取分类
        if (bill.getCategoryId() != null) {
            Category category = categoryService.getById(bill.getCategoryId());
            billVO.setCategory(BeanUtil.copyProperties(category, CategoryVO.class));
        }

        // 单个查询：直接获取支付方式
        if (bill.getPaymentMethodId() != null) {
            PaymentMethod paymentMethod = paymentMethodService.getById(bill.getPaymentMethodId());
            billVO.setPaymentMethod(BeanUtil.copyProperties(paymentMethod, PaymentMethodVO.class));
        }

        billVO.setType(bill.getType());
        billVO.setAmount(bill.getAmount());
        billVO.setTime(bill.getTime());
        billVO.setRemark(bill.getRemark());
        billVO.setSource(bill.getSource());
        return billVO;
    }

    /**
     * 批量转换 Bill 为 BillVO（适用于列表分页查询场景）
     * 使用预加载的 Map 避免 N+1 查询问题
     */
    private BillVO getBillVO(
            Bill bill, Map<String, Category> categoryMap, Map<String, PaymentMethod> paymentMethodMap) {
        BillVO billVO = new BillVO();
        billVO.setId(bill.getId());
        billVO.setFamilyId(bill.getFamilyId());
        billVO.setName(bill.getName());

        // 从缓存的 Map 中获取分类名称
        Category category = categoryMap.get(bill.getCategoryId());
        billVO.setCategory(BeanUtil.copyProperties(category, CategoryVO.class));

        // 从缓存的 Map 中获取支付方式
        PaymentMethod paymentMethod = paymentMethodMap.get(bill.getPaymentMethodId());
        billVO.setPaymentMethod(BeanUtil.copyProperties(paymentMethod, PaymentMethodVO.class));

        billVO.setType(bill.getType());
        billVO.setAmount(bill.getAmount());
        billVO.setTime(bill.getTime());
        billVO.setRemark(bill.getRemark());
        billVO.setSource(bill.getSource());
        return billVO;
    }

    private List<BillVO> convertToBillVOList(List<Bill> billList) {
        Map<String, Category> categoryMap = batchQueryCategories(billList);
        Map<String, PaymentMethod> paymentMethodMap = batchQueryPaymentMethods(billList);
        return billList.stream()
                .map(bill -> getBillVO(bill, categoryMap, paymentMethodMap))
                .toList();
    }

    private IPage<BillVO> convertToBillVOPage(IPage<Bill> billPage) {
        Map<String, Category> categoryMap = batchQueryCategories(billPage.getRecords());
        Map<String, PaymentMethod> paymentMethodMap = batchQueryPaymentMethods(billPage.getRecords());
        return billPage.convert(bill -> getBillVO(bill, categoryMap, paymentMethodMap));
    }

    private Map<String, Category> batchQueryCategories(List<Bill> billList) {
        List<String> categoryIds = billList.stream()
                .map(Bill::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isNotEmpty(categoryIds)) {
            List<Category> categories = categoryService.listByIds(categoryIds);
            return categories.stream()
                    .collect(Collectors.toMap(
                            Category::getId, Function.identity(), (existing, replacement) -> existing));
        }
        return Collections.emptyMap();
    }

    private Map<String, PaymentMethod> batchQueryPaymentMethods(List<Bill> billList) {
        List<String> paymentMethodIds = billList.stream()
                .map(Bill::getPaymentMethodId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isNotEmpty(paymentMethodIds)) {
            List<PaymentMethod> paymentMethods = paymentMethodService.listByIds(paymentMethodIds);
            return paymentMethods.stream()
                    .collect(Collectors.toMap(
                            PaymentMethod::getId, Function.identity(), (existing, replacement) -> existing));
        }
        return Collections.emptyMap();
    }
}
