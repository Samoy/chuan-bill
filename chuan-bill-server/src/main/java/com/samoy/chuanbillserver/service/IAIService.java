package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.vo.AiAnalysisVO;
import com.samoy.chuanbillserver.vo.BillVO;

public interface IAIService {

    /**
     * 通过文件id进行ocr识别
     *
     * @param fileId 文件id
     * @param fileExt 文件扩展名
     * @return 识别结果
     */
    BillVO ocr(String fileId, String fileExt);

    /**
     * 通过文本进行识别
     *
     * @param text 文本
     * @return 识别结果
     */
    BillVO text(String text);

    /**
     * 通过月份进行账单分析
     * @param analysisType 分析类型
     * @param month 月份
     * @param familyId 家庭id
     * @param regenerate 是否重新生成
     * @return 分析结果
     */
    AiAnalysisVO analysis(Integer analysisType, String month, String familyId, boolean regenerate);
}
