package com.samoy.chuanbillserver.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samoy.chuanbillserver.entity.Budget;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;

public interface BudgetMapper extends BaseMapper<Budget> {

    /**
     * 实时计算用户指定月份的支出总额
     */
    BigDecimal getMonthlyExpense(@Param("userId") String userId, @Param("month") String month);
}
