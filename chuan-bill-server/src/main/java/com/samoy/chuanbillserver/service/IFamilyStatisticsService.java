package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.dto.FamilyMemberStatsDTO;
import com.samoy.chuanbillserver.vo.FamilyAiSuggestionVO;
import com.samoy.chuanbillserver.vo.FamilyMemberStatsVO;
import java.util.List;

/**
 * <p>
 * 家庭统计服务接口
 * </p>
 *
 * @author samoy
 * @since 2026/4/21
 */
public interface IFamilyStatisticsService {

    /**
     * 获取家庭成员收支统计
     *
     * @param userId 当前用户ID
     * @param dto    查询参数
     * @return 成员统计列表
     */
    List<FamilyMemberStatsVO> getMemberStats(String userId, FamilyMemberStatsDTO dto);

    /**
     * 获取家庭AI建议（仅户主可用）
     *
     * @param userId 当前用户ID
     * @param dto    查询参数
     * @return AI建议
     */
    FamilyAiSuggestionVO getAiSuggestion(String userId, FamilyMemberStatsDTO dto);
}
