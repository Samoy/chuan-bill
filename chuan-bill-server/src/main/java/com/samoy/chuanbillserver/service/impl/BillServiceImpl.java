package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.dto.AddBillDTO;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.UpdateBillDTO;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.entity.Category;
import com.samoy.chuanbillserver.entity.PaymentMethod;
import com.samoy.chuanbillserver.expection.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.service.ICategoryService;
import com.samoy.chuanbillserver.service.IPaymentMethodService;
import com.samoy.chuanbillserver.vo.BillVO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    public IPage<BillVO> getBillList(String userId, BillListDTO billListDTO) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId).orderByDesc(Bill::getTime, Bill::getCreateTime);

        // 日期范围查询
        if (billListDTO.getStartDate() != null) {
            wrapper.ge(Bill::getTime, LocalDateTime.parse(String.format("%sT00:00:00", billListDTO.getStartDate())));
        }
        if (billListDTO.getEndDate() != null) {
            wrapper.le(Bill::getTime, LocalDateTime.parse(String.format("%sT23:59:59", billListDTO.getEndDate())));
        }

        // 类型过滤
        if (billListDTO.getType() != null) {
            wrapper.eq(Bill::getType, billListDTO.getType());
        }

        // 分类过滤
        if (billListDTO.getCategoryId() != null) {
            wrapper.eq(Bill::getCategoryId, billListDTO.getCategoryId());
        }

        // 金额范围过滤
        if (billListDTO.getMinAmount() != null) {
            wrapper.ge(Bill::getAmount, billListDTO.getMinAmount());
        }
        if (billListDTO.getMaxAmount() != null) {
            wrapper.le(Bill::getAmount, billListDTO.getMaxAmount());
        }
        // 名称模糊查询
        if (billListDTO.getName() != null) {
            wrapper.like(Bill::getName, billListDTO.getName().trim());
        }
        // 备注模糊查询
        if (billListDTO.getRemark() != null) {
            wrapper.like(Bill::getRemark, billListDTO.getRemark().trim());
        }
        IPage<Bill> billPage = this.page(new Page<>(), wrapper);

        if (billPage.getRecords().isEmpty()) {
            return new Page<>();
        }

        // 批量查询分类信息
        List<String> categoryIds = billPage.getRecords().stream()
                .map(Bill::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Category> categoryMap;
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryService.listByIds(categoryIds);
            categoryMap =
                    categories.stream().collect(Collectors.toMap(Category::getId, Function.identity(), (v1, v2) -> v1));
        } else {
            categoryMap = new HashMap<>();
        }

        // 批量查询支付方式信息
        List<String> paymentMethodIds = billPage.getRecords().stream()
                .map(Bill::getPaymentMethodId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, PaymentMethod> paymentMethodMap;
        if (!paymentMethodIds.isEmpty()) {
            List<PaymentMethod> paymentMethods = paymentMethodService.listByIds(paymentMethodIds);
            paymentMethodMap = paymentMethods.stream()
                    .collect(Collectors.toMap(PaymentMethod::getId, Function.identity(), (v1, v2) -> v1));
        } else {
            paymentMethodMap = new HashMap<>();
        }

        return billPage.convert(bill -> this.getBillVO(bill, categoryMap, paymentMethodMap));
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

    /**
     * 转换单个 Bill 为 BillVO（适用于单个查询场景）
     */
    private BillVO getBillVO(Bill bill) {
        BillVO billVO = new BillVO();
        billVO.setId(bill.getId());
        billVO.setFamilyId(bill.getFamilyId());
        billVO.setName(bill.getName());
        billVO.setCategoryId(bill.getCategoryId());

        // 单个查询：直接获取分类名称
        if (bill.getCategoryId() != null) {
            Category category = categoryService.getById(bill.getCategoryId());
            billVO.setCategoryName(category != null ? category.getName() : null);
        }

        billVO.setPaymentMethodId(bill.getPaymentMethodId());

        // 单个查询：直接获取支付方式名称
        if (bill.getPaymentMethodId() != null) {
            PaymentMethod paymentMethod = paymentMethodService.getById(bill.getPaymentMethodId());
            billVO.setPaymentMethodName(paymentMethod != null ? paymentMethod.getName() : null);
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
        billVO.setCategoryId(bill.getCategoryId());

        // 从缓存的 Map 中获取分类名称
        Category category = categoryMap.get(bill.getCategoryId());
        billVO.setCategoryName(category != null ? category.getName() : null);

        billVO.setPaymentMethodId(bill.getPaymentMethodId());

        // 从缓存的 Map 中获取支付方式名称
        PaymentMethod paymentMethod = paymentMethodMap.get(bill.getPaymentMethodId());
        billVO.setPaymentMethodName(paymentMethod != null ? paymentMethod.getName() : null);

        billVO.setType(bill.getType());
        billVO.setAmount(bill.getAmount());
        billVO.setTime(bill.getTime());
        billVO.setRemark(bill.getRemark());
        billVO.setSource(bill.getSource());
        return billVO;
    }
}
