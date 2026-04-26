package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.dao.AiSuggestionMapper;
import com.samoy.chuanbillserver.entity.AiSuggestion;
import com.samoy.chuanbillserver.service.IAiSuggestionService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * AI分析建议表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-04-18
 */
@Service
public class AiSuggestionServiceImpl extends ServiceImpl<AiSuggestionMapper, AiSuggestion>
        implements IAiSuggestionService {

    @Override
    public AiSuggestion getByUserIdAndMonth(Integer analysisType, String userId, String familyId, String month) {
        LambdaQueryWrapper<AiSuggestion> wrapper = new LambdaQueryWrapper<>();
        if (analysisType == SystemConstants.USER_ANALYSIS_TYPE) {
            // 说明是个人账单分析
            wrapper.eq(AiSuggestion::getTargetId, userId);
        }
        if (analysisType == SystemConstants.FAMILY_ANALYSIS_TYPE) {
            // 说明是家庭账单分析
            wrapper.eq(AiSuggestion::getTargetId, familyId);
        }
        return getOne(wrapper.eq(AiSuggestion::getUserId, userId)
                .eq(AiSuggestion::getAnalysisType, analysisType)
                .eq(AiSuggestion::getMonth, month));
    }

    @Override
    public void saveOrUpdateSuggestion(Integer analysis, String userId, String familyId, String month, String content) {
        AiSuggestion existing = getByUserIdAndMonth(analysis, userId, familyId, month);
        if (existing != null) {
            existing.setContent(content);
            updateById(existing);
        } else {
            AiSuggestion suggestion = new AiSuggestion();
            suggestion.setUserId(userId);
            suggestion.setMonth(month);
            suggestion.setAnalysisType(analysis);
            if (analysis == SystemConstants.USER_ANALYSIS_TYPE) {
                suggestion.setTargetId(userId);
            }
            if (analysis == SystemConstants.FAMILY_ANALYSIS_TYPE) {
                suggestion.setTargetId(familyId);
            }
            suggestion.setContent(content);
            save(suggestion);
        }
    }
}
