package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.dao.AiUsageMapper;
import com.samoy.chuanbillserver.entity.AiUsage;
import com.samoy.chuanbillserver.service.IAiUsageService;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * <p>
 * AI使用次数统计表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-04-18
 */
@Service
public class AiUsageServiceImpl extends ServiceImpl<AiUsageMapper, AiUsage> implements IAiUsageService {

    @Override
    public int getRemainingCount(String userId, boolean isVip) {
        if (isVip) {
            return -1;
        }
        AiUsage usage = getOne(new LambdaQueryWrapper<AiUsage>()
                .eq(AiUsage::getUserId, userId)
                .eq(AiUsage::getUsageDate, LocalDate.now()));
        if (usage == null) {
            return SystemConstants.AI_DAILY_LIMIT_NORMAL;
        }
        return Math.max(0, SystemConstants.AI_DAILY_LIMIT_NORMAL - usage.getAnalysisCount());
    }

    @Override
    public void incrementUsage(String userId) {
        baseMapper.incrementAnalysisCount(userId, LocalDate.now());
    }
}
