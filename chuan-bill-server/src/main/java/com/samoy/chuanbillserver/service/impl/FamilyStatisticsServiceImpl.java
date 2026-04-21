package com.samoy.chuanbillserver.service.impl;

import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.dto.FamilyMemberStatsDTO;
import com.samoy.chuanbillserver.entity.User;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IAiUsageService;
import com.samoy.chuanbillserver.service.IFamilyService;
import com.samoy.chuanbillserver.service.IFamilyStatisticsService;
import com.samoy.chuanbillserver.service.IUserService;
import com.samoy.chuanbillserver.vo.FamilyAiSuggestionVO;
import com.samoy.chuanbillserver.vo.FamilyMemberStatsVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 家庭统计服务实现类
 * </p>
 *
 * @author samoy
 * @since 2026/4/21
 */
@Slf4j
@Service
public class FamilyStatisticsServiceImpl implements IFamilyStatisticsService {

    @Resource
    private BillMapper billMapper;

    @Resource
    private IFamilyService familyService;

    @Resource
    private IAiUsageService aiUsageService;

    @Resource
    private IUserService userService;

    @Override
    public List<FamilyMemberStatsVO> getMemberStats(String userId, FamilyMemberStatsDTO dto) {
        // 1. 校验用户是家庭成员
        if (!familyService.isMember(userId, dto.getFamilyId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }

        // 2. 计算时间范围
        YearMonth yearMonth = YearMonth.parse(dto.getMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime startTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 3. 查询成员统计
        List<FamilyMemberStatsVO> list = billMapper.selectMemberStats(dto.getFamilyId(), startTime, endTime);

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 计算总支出和总收入
        BigDecimal totalExpense =
                list.stream().map(FamilyMemberStatsVO::getExpense).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncome =
                list.stream().map(FamilyMemberStatsVO::getIncome).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. 计算百分比
        for (FamilyMemberStatsVO item : list) {
            item.setExpensePercentage(calculatePercentage(item.getExpense(), totalExpense));
            item.setIncomePercentage(calculatePercentage(item.getIncome(), totalIncome));
            // 处理isOwner为null的情况
            if (item.getIsOwner() == null) {
                item.setIsOwner(false);
            }
        }

        return list;
    }

    @Override
    public FamilyAiSuggestionVO getAiSuggestion(String userId, FamilyMemberStatsDTO dto) {
        // 1. 校验用户是户主
        if (!familyService.isOwner(userId, dto.getFamilyId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_OWNER);
        }

        // 2. 获取用户VIP状态
        User user = userService.getById(userId);
        boolean isVip = Boolean.TRUE.equals(user.getIsVip());

        // 3. 检查剩余次数
        int remainingCount = aiUsageService.getRemainingCount(userId, isVip);

        // 4. 构建返回对象
        FamilyAiSuggestionVO vo = new FamilyAiSuggestionVO();
        vo.setRemainingCount(remainingCount);

        // 5. 如果没有剩余次数且非VIP，返回提示
        if (remainingCount <= 0 && !isVip) {
            vo.setContent("今日AI建议次数已用完，请明天再试或升级VIP享受无限次数。");
            vo.setCached(false);
            return vo;
        }

        // 6. 返回固定文本（Agent暂未搭建）
        vo.setContent(generateFixedSuggestion(dto.getFamilyId(), dto.getMonth()));
        vo.setCached(true);

        // 7. 记录使用次数（VIP也记录但不受限）
        aiUsageService.incrementUsage(userId);

        return vo;
    }

    /**
     * 计算百分比
     */
    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("0.00");
        }
        return amount.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成固定建议文本
     */
    private String generateFixedSuggestion(String familyId, String month) {
        return String.format(
                "【%s家庭账单分析】\n\n" + "1. 建议定期核对家庭收支，确保每笔账单记录准确\n"
                        + "2. 大额支出建议提前与家庭成员沟通协商\n"
                        + "3. 可以设置月度预算目标，控制非必要开支\n"
                        + "4. 鼓励家庭成员共同参与记账，提高财务透明度\n\n"
                        + "完整AI分析功能即将上线，敬请期待！",
                month);
    }
}
