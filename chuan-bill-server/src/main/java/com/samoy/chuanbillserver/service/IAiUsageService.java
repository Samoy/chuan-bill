package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.entity.AiUsage;

/**
 * <p>
 * AI使用次数统计表 服务类
 * </p>
 *
 * @author Samoy
 * @since 2026-04-18
 */
public interface IAiUsageService extends IService<AiUsage> {

    /**
     * 获取今日剩余AI分析次数
     *
     * @param userId 用户ID
     * @param isVip 是否VIP
     * @return 剩余次数，VIP返回-1表示无限制
     */
    int getRemainingCount(String userId, boolean isVip);

    /**
     * 递增当日AI分析调用次数
     *
     * @param userId 用户ID
     */
    void incrementUsage(String userId);
}
