package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.dto.AddBillDTO;
import com.samoy.chuanbillserver.dto.BatchCreateBillDTO;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.BillMonthlyStatsDTO;
import com.samoy.chuanbillserver.dto.UpdateBillDTO;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.entity.Category;
import com.samoy.chuanbillserver.entity.Family;
import com.samoy.chuanbillserver.entity.PaymentMethod;
import com.samoy.chuanbillserver.entity.User;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.*;
import com.samoy.chuanbillserver.vo.BatchSyncResultVO;
import com.samoy.chuanbillserver.vo.BillMonthlyStatsVO;
import com.samoy.chuanbillserver.vo.BillSyncDetailVO;
import com.samoy.chuanbillserver.vo.BillVO;
import com.samoy.chuanbillserver.vo.CategoryVO;
import com.samoy.chuanbillserver.vo.FamilyMemberVO;
import com.samoy.chuanbillserver.vo.PaymentMethodVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 账单表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Slf4j
@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements IBillService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ICategoryService categoryService;

    @Resource
    private IPaymentMethodService paymentMethodService;

    @Resource
    private IFamilyService familyService;

    @Resource
    private IUserService userService;

    @Resource
    private IMessageService messageService;

    @Override
    public List<BillVO> getBillList(String userId, BillListDTO billListDTO) {
        if (billListDTO.getFamilyId() != null && !familyService.isMember(userId, billListDTO.getFamilyId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }
        LambdaQueryWrapper<Bill> wrapper = buildQueryWrapper(userId, billListDTO);
        List<Bill> billList = baseMapper.selectList(wrapper);
        return convertToBillVOList(billList);
    }

    @Override
    public IPage<BillVO> getBillListByPage(String userId, BillListDTO billListDTO) {
        // 如果是家庭账单，则需要查询是否是家庭成员
        if (billListDTO.getFamilyId() != null && !familyService.isMember(userId, billListDTO.getFamilyId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }
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
            if (!familyService.isMember(userId, addBillDTO.getFamilyId())) {
                throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
            }
            bill.setFamilyId(addBillDTO.getFamilyId());
        }
        boolean saved = this.save(bill);
        // 家庭账单通知：通知家庭其他成员
        if (saved && bill.getFamilyId() != null) {
            sendFamilyBillNotification(bill, userId);
        }
        return saved;
    }

    @Override
    public BatchSyncResultVO batchCreate(String userId, BatchCreateBillDTO dto) {
        List<AddBillDTO> bills = dto.getBills();
        if (bills == null || bills.isEmpty()) {
            return BatchSyncResultVO.of(Collections.emptyList(), 0);
        }

        // 逐条保存而非批量，以支持部分成功语义：单条失败不影响其他账单的持久化
        List<BillSyncDetailVO> details = new ArrayList<>();
        for (int i = 0; i < bills.size(); i++) {
            AddBillDTO addBillDTO = bills.get(i);
            try {
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
                this.save(bill);
                details.add(BillSyncDetailVO.success(i, bill.getId()));
            } catch (Exception e) {
                log.warn("账单同步失败，索引: {}", i, e);
                String reason = e.getMessage();
                if (reason != null && reason.length() > 200) {
                    reason = reason.substring(0, 200);
                }
                details.add(BillSyncDetailVO.failed(i, reason));
            }
        }
        return BatchSyncResultVO.of(details, bills.size());
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
        if (updateBillDTO.getFamilyId() != null) {
            bill.setFamilyId(updateBillDTO.getFamilyId());
        }
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
            // 如果是家庭共享账单，检查是否为家庭成员
            if (bill.getFamilyId() == null || !familyService.isMember(userId, bill.getFamilyId())) {
                throw new BusinessException(ResultEnum.BILL_NOT_ALLOW_VIEW);
            }
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

    /**
     * 发送家庭账单通知给家庭其他成员
     */
    private void sendFamilyBillNotification(Bill bill, String userId) {
        try {
            List<FamilyMemberVO> members = familyService.getMembers(userId, bill.getFamilyId());

            // 获取记账人昵称
            String nickname = members.stream()
                    .filter(m -> m.getUserId().equals(userId))
                    .findFirst()
                    .map(FamilyMemberVO::getUserNickname)
                    .orElse("未知用户");

            // 获取分类名
            Category category = categoryService.getById(bill.getCategoryId());
            String categoryName = category != null ? category.getName() : "未分类";

            Map<String, String> contentMap = new HashMap<>();
            contentMap.put("categoryName", categoryName);
            contentMap.put("amount", bill.getAmount().toPlainString());
            contentMap.put("type", bill.getType().toString());
            String content = objectMapper.writeValueAsString(contentMap);

            for (FamilyMemberVO member : members) {
                if (member.getUserId().equals(userId)) {
                    continue;
                }
                messageService.sendMessage(
                        member.getUserId(), nickname + " 记了一笔账单", content, "bill", bill.getId(), "bill");
            }
        } catch (Exception e) {
            log.error("发送家庭账单通知失败，账单ID: {}", bill.getId(), e);
        }
    }

    private LambdaQueryWrapper<Bill> buildQueryWrapper(String userId, BillListDTO billListDTO) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();

        // 家庭账单查询：按 familyId 过滤；否则按用户过滤
        if (ObjectUtil.isNotEmpty(billListDTO.getFamilyId())) {
            wrapper.eq(Bill::getFamilyId, billListDTO.getFamilyId());
        } else {
            wrapper.eq(Bill::getUserId, userId);
        }
        wrapper.orderByDesc(Bill::getTime, Bill::getCreateTime);

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

        // 填充家庭名称
        if (bill.getFamilyId() != null) {
            Family family = familyService.getById(bill.getFamilyId());
            if (family != null) {
                billVO.setFamilyName(family.getName());
            }
        }

        return billVO;
    }

    /**
     * 批量转换 Bill 为 BillVO（适用于列表分页查询场景）
     * 使用预加载的 Map 避免 N+1 查询问题
     */
    private BillVO getBillVO(
            Bill bill,
            Map<String, Category> categoryMap,
            Map<String, PaymentMethod> paymentMethodMap,
            Map<String, Family> familyMap,
            Map<String, User> userMap) {
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

        // 填充家庭名称
        Family family = familyMap.get(bill.getFamilyId());
        if (family != null) {
            billVO.setFamilyName(family.getName());
        }

        // 填充记账人信息
        User user = userMap.get(bill.getUserId());
        if (user != null) {
            billVO.setUserId(user.getId());
            billVO.setUserNickname(user.getNickname());
            billVO.setUserAvatar(user.getAvatar());
        }

        return billVO;
    }

    private List<BillVO> convertToBillVOList(List<Bill> billList) {
        Map<String, Category> categoryMap = batchQueryCategories(billList);
        Map<String, PaymentMethod> paymentMethodMap = batchQueryPaymentMethods(billList);
        Map<String, Family> familyMap = batchQueryFamilies(billList);
        Map<String, User> userMap = batchQueryUsers(billList);
        return billList.stream()
                .map(bill -> getBillVO(bill, categoryMap, paymentMethodMap, familyMap, userMap))
                .toList();
    }

    private IPage<BillVO> convertToBillVOPage(IPage<Bill> billPage) {
        Map<String, Category> categoryMap = batchQueryCategories(billPage.getRecords());
        Map<String, PaymentMethod> paymentMethodMap = batchQueryPaymentMethods(billPage.getRecords());
        Map<String, Family> familyMap = batchQueryFamilies(billPage.getRecords());
        Map<String, User> userMap = batchQueryUsers(billPage.getRecords());
        return billPage.convert(bill -> getBillVO(bill, categoryMap, paymentMethodMap, familyMap, userMap));
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

    private Map<String, Family> batchQueryFamilies(List<Bill> billList) {
        List<String> familyIds = billList.stream()
                .map(Bill::getFamilyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isNotEmpty(familyIds)) {
            List<Family> families = familyService.listByIds(familyIds);
            return families.stream()
                    .collect(Collectors.toMap(Family::getId, Function.identity(), (existing, replacement) -> existing));
        }
        return Collections.emptyMap();
    }

    private Map<String, User> batchQueryUsers(List<Bill> billList) {
        List<String> userIds = billList.stream()
                .map(Bill::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isNotEmpty(userIds)) {
            List<User> users = userService.listByIds(userIds);
            return users.stream()
                    .collect(Collectors.toMap(User::getId, Function.identity(), (existing, replacement) -> existing));
        }
        return Collections.emptyMap();
    }
}
