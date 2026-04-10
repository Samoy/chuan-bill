package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.vo.BillVO;

public interface IAIService {

    /**
     * 通过文件id进行ocr识别
     *
     * @param fileId 文件id
     * @return 识别结果
     */
    BillVO ocr(String fileId);

    /**
     * 通过文本进行识别
     *
     * @param text 文本
     * @return 识别结果
     */
    BillVO text(String text);

    /**
     * 通过月份进行账单分析
     *
     * @param month 月份
     * @return 分析结果
     */
    String analysis(String month);
}
