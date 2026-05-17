package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.BudgetMapper;
import com.samoy.chuanbillserver.dto.SetBudgetDTO;
import com.samoy.chuanbillserver.entity.Budget;
import com.samoy.chuanbillserver.entity.Message;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IBudgetService;
import com.samoy.chuanbillserver.service.IMessageService;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import com.samoy.chuanbillserver.vo.BudgetVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget> implements IBudgetService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private IMessageService messageService;

    @Resource
    private IUserPreferenceService userPreferenceService;

    @Override
    public BudgetVO getCurrentBudget(String userId, String month) {
        LocalDate monthDate = parseMonth(month);
        Budget budget = getOne(
                new LambdaQueryWrapper<Budget>().eq(Budget::getUserId, userId).eq(Budget::getMonth, monthDate));
        if (budget == null) {
            return null;
        }
        return convertToVO(budget, userId, month);
    }

    @Override
    public BudgetVO setBudget(String userId, SetBudgetDTO dto) {
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        LocalDate monthDate = parseMonth(currentMonth);

        Budget existing = getOne(
                new LambdaQueryWrapper<Budget>().eq(Budget::getUserId, userId).eq(Budget::getMonth, monthDate));

        if (existing != null) {
            existing.setAmount(dto.getAmount());
            this.updateById(existing);
            return convertToVO(existing, userId, currentMonth);
        } else {
            Budget budget = new Budget();
            budget.setId(UUID.randomUUID().toString().replace("-", ""));
            budget.setUserId(userId);
            budget.setMonth(monthDate);
            budget.setAmount(dto.getAmount());
            budget.setUseAmount(BigDecimal.ZERO);
            this.save(budget);
            return convertToVO(budget, userId, currentMonth);
        }
    }

    @Override
    public boolean deleteBudget(String userId) {
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        LocalDate monthDate = parseMonth(currentMonth);

        Budget budget = getOne(
                new LambdaQueryWrapper<Budget>().eq(Budget::getUserId, userId).eq(Budget::getMonth, monthDate));
        if (budget == null) {
            throw new BusinessException(ResultEnum.BUDGET_NOT_FOUND);
        }
        return this.removeById(budget.getId());
    }

    @Override
    public void checkBudgetAlert(String userId) {
        // 1. 检查通知总开关（null 视为默认开启）
        String masterEnabled = userPreferenceService.getValue(userId, "notification.master.enabled");
        if (masterEnabled != null && "false".equals(masterEnabled)) {
            return;
        }

        // 2. 检查预算提醒开关（null 视为默认开启）
        String budgetEnabled = userPreferenceService.getValue(userId, "notification.budget.enabled");
        if (budgetEnabled != null && "false".equals(budgetEnabled)) {
            return;
        }

        // 3. 查询当月预算
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        LocalDate monthDate = parseMonth(currentMonth);
        Budget budget = getOne(
                new LambdaQueryWrapper<Budget>().eq(Budget::getUserId, userId).eq(Budget::getMonth, monthDate));
        if (budget == null) {
            return;
        }

        // 4. 实时计算当月支出
        BigDecimal expense = baseMapper.getMonthlyExpense(userId, currentMonth);
        if (expense == null) {
            expense = BigDecimal.ZERO;
        }

        // 5. 计算使用率
        BigDecimal usagePercent =
                expense.multiply(BigDecimal.valueOf(100)).divide(budget.getAmount(), 2, RoundingMode.HALF_UP);

        // 6. 判断阈值并发送通知
        if (usagePercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            sendBudgetNotificationIfNotExists(
                    userId,
                    budget.getId(),
                    "预算超支",
                    String.format(
                            "本月支出已超出预算 ¥%s，请合理安排消费",
                            expense.subtract(budget.getAmount()).setScale(2, RoundingMode.HALF_UP)));
        } else if (usagePercent.compareTo(BigDecimal.valueOf(80)) >= 0) {
            sendBudgetNotificationIfNotExists(
                    userId,
                    budget.getId(),
                    "预算预警",
                    String.format("本月支出已达预算的 %s%%，请注意控制开支", usagePercent.setScale(0, RoundingMode.HALF_UP)));
        }
    }

    private void sendBudgetNotificationIfNotExists(String userId, String budgetId, String title, String content) {
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        // 检查当月是否已发送过同类型通知
        long count = messageService.count(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getType, "budget")
                .eq(Message::getTitle, title)
                .apply("DATE_FORMAT(create_time, '%Y-%m') = {0}", currentMonth));
        if (count == 0) {
            messageService.sendMessage(userId, title, content, "budget", budgetId, "budget");
        }
    }

    private BudgetVO convertToVO(Budget budget, String userId, String month) {
        BigDecimal expense = baseMapper.getMonthlyExpense(userId, month);
        if (expense == null) {
            expense = BigDecimal.ZERO;
        }

        BudgetVO vo = new BudgetVO();
        vo.setId(budget.getId());
        vo.setUserId(budget.getUserId());
        vo.setMonth(month);
        vo.setAmount(budget.getAmount());
        vo.setUseAmount(expense);
        vo.setRemainingAmount(budget.getAmount().subtract(expense));
        vo.setUsagePercent(
                expense.multiply(BigDecimal.valueOf(100)).divide(budget.getAmount(), 2, RoundingMode.HALF_UP));
        vo.setCreateTime(budget.getCreateTime());
        vo.setUpdateTime(budget.getUpdateTime());
        return vo;
    }

    private LocalDate parseMonth(String month) {
        return YearMonth.parse(month, MONTH_FMT).atDay(1);
    }
}
