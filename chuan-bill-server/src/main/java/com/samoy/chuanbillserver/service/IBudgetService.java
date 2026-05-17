package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.dto.SetBudgetDTO;
import com.samoy.chuanbillserver.entity.Budget;
import com.samoy.chuanbillserver.vo.BudgetVO;

public interface IBudgetService extends IService<Budget> {

    /**
     * 获取指定月份的预算信息（含实时计算的已用金额）
     */
    BudgetVO getCurrentBudget(String userId, String month);

    /**
     * 设置或修改当月预算
     */
    BudgetVO setBudget(String userId, SetBudgetDTO dto);

    /**
     * 删除当月预算
     */
    boolean deleteBudget(String userId);

    /**
     * 检查预算预警并发送通知（供 BillServiceImpl 调用）
     */
    void checkBudgetAlert(String userId);
}
