package com.samoy.chuanbillserver.service.impl;

import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.dto.BillMonthlyStatsDTO;
import com.samoy.chuanbillserver.dto.StatisticsCategoryDTO;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.service.IFamilyService;
import com.samoy.chuanbillserver.service.IStatisticsService;
import com.samoy.chuanbillserver.vo.BillMonthlyStatsVO;
import com.samoy.chuanbillserver.vo.CategoryStatisticsVO;
import com.samoy.chuanbillserver.vo.DailyTrendVO;
import com.samoy.chuanbillserver.vo.FamilyMemberStatsVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 统计分析服务实现类
 * </p>
 *
 * @author samoy
 * @since 2026/4/17
 */
@Service
public class StatisticsServiceImpl implements IStatisticsService {

    @Resource
    private BillMapper billMapper;

    @Resource
    private IBillService billService;

    @Resource
    private IFamilyService familyService;

    @Override
    public BillMonthlyStatsVO getOverview(String userId, BillMonthlyStatsDTO dto) {
        // 如果是查询家庭，需要判断是否是家庭成员
        testFamilyMember(userId, dto.getFamilyId());
        return billService.getMonthlyStats(userId, dto);
    }

    @Override
    public List<CategoryStatisticsVO> getCategoryStats(String userId, StatisticsCategoryDTO dto) {
        // 如果是查询家庭，需要判断是否是家庭成员
        testFamilyMember(userId, dto.getFamilyId());
        YearMonth yearMonth = YearMonth.parse(dto.getMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime startTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<CategoryStatisticsVO> list =
                billMapper.selectCategoryStats(userId, dto.getFamilyId(), dto.getType(), startTime, endTime);

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        // 计算总金额
        BigDecimal totalAmount =
                list.stream().map(CategoryStatisticsVO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算每项百分比
        for (CategoryStatisticsVO item : list) {
            if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                item.setPercentage(new BigDecimal("0.00"));
            } else {
                BigDecimal percentage = item.getAmount()
                        .divide(totalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
                item.setPercentage(percentage);
            }
        }

        return list;
    }

    @Override
    public List<DailyTrendVO> getDailyTrend(String userId, BillMonthlyStatsDTO dto) {
        // 如果是查询家庭，需要判断是否是家庭成员
        testFamilyMember(userId, dto.getFamilyId());
        YearMonth yearMonth = YearMonth.parse(dto.getMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime startTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<DailyTrendVO> rawList = billMapper.selectDailyTrend(userId, dto.getFamilyId(), startTime, endTime);

        // 转换为Map方便查找
        Map<String, DailyTrendVO> trendMap = rawList == null
                ? Map.of()
                : rawList.stream().collect(Collectors.toMap(DailyTrendVO::getDate, Function.identity(), (a, b) -> a));

        // 填充缺失的日期
        List<DailyTrendVO> result = new ArrayList<>();
        int daysInMonth = yearMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            String date = String.format("%s-%02d", dto.getMonth(), day);
            DailyTrendVO vo = trendMap.get(date);
            if (vo != null) {
                // 处理可能的null值
                vo.setExpense(vo.getExpense() == null ? new BigDecimal("0.00") : vo.getExpense());
                vo.setIncome(vo.getIncome() == null ? new BigDecimal("0.00") : vo.getIncome());
                result.add(vo);
            } else {
                DailyTrendVO empty = new DailyTrendVO();
                empty.setDate(date);
                empty.setExpense(new BigDecimal("0.00"));
                empty.setIncome(new BigDecimal("0.00"));
                result.add(empty);
            }
        }

        return result;
    }

    @Override
    public List<FamilyMemberStatsVO> getMembersStats(String userId, BillMonthlyStatsDTO dto) {
        // 1. 如果是查询家庭，需要判断是否是家庭成员
        testFamilyMember(userId, dto.getFamilyId());

        // 2. 计算时间范围
        YearMonth yearMonth = YearMonth.parse(dto.getMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime startTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 3. 查询成员统计
        List<FamilyMemberStatsVO> list = billMapper.selectMemberStats(dto.getFamilyId(), startTime, endTime);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 计算总支出和总输入
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

    private void testFamilyMember(String userId, String familyId) {
        if (familyId != null && !familyService.isMember(userId, familyId)) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("0.00");
        }
        return amount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
