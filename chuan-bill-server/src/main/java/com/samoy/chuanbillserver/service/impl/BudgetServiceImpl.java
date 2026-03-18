package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.BudgetMapper;
import com.samoy.chuanbillserver.entity.Budget;
import com.samoy.chuanbillserver.service.IBudgetService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 预算表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Service
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget> implements IBudgetService {}
