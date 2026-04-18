package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.entity.AiSuggestion;

/**
 * <p>
 * AI分析建议表 服务类
 * </p>
 *
 * @author Samoy
 * @since 2026-04-18
 */
public interface IAiSuggestionService extends IService<AiSuggestion> {

    /**
     * 根据用户ID和月份查询AI建议
     *
     * @param userId 用户ID
     * @param month 月份，格式YYYY-MM
     * @return AI建议
     */
    AiSuggestion getByUserIdAndMonth(String userId, String month);

    /**
     * 保存或更新AI建议
     *
     * @param userId 用户ID
     * @param month 月份，格式YYYY-MM
     * @param content AI分析内容
     */
    void saveOrUpdateSuggestion(String userId, String month, String content);
}
