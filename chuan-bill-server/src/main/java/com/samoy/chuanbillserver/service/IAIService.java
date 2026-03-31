package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.dto.AddBillDTO;

public interface IAIService {

    /**
     * 通过文件id进行ocr识别
     * @param fileId 文件id
     * @return 识别结果
     */
    AddBillDTO ocr(String fileId);
}
