package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
    public AiSuggestion getByUserIdAndMonth(String userId, String month) {
        return getOne(new LambdaQueryWrapper<AiSuggestion>()
                .eq(AiSuggestion::getUserId, userId)
                .eq(AiSuggestion::getMonth, month));
    }

    @Override
    public void saveOrUpdateSuggestion(String userId, String month, String content) {
        AiSuggestion existing = getByUserIdAndMonth(userId, month);
        if (existing != null) {
            existing.setContent(content);
            existing.setDeleted(false);
            updateById(existing);
        } else {
            AiSuggestion suggestion = new AiSuggestion();
            suggestion.setUserId(userId);
            suggestion.setMonth(month);
            suggestion.setContent(content);
            suggestion.setDeleted(false);
            save(suggestion);
        }
    }
}
