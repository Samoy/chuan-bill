package com.samoy.chuanbillserver.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samoy.chuanbillserver.entity.AiUsage;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * AI使用次数统计表 Mapper 接口
 * </p>
 *
 * @author Samoy
 * @since 2026-04-18
 */
public interface AiUsageMapper extends BaseMapper<AiUsage> {

    /**
     * 原子递增当日AI分析调用次数，不存在则插入
     *
     * @param userId 用户ID
     * @param usageDate 使用日期
     */
    void incrementAnalysisCount(@Param("userId") String userId, @Param("usageDate") LocalDate usageDate);
}
