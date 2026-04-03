package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.vo.BillVO;

public interface IAIService {

    /**
     * 通过文件id进行ocr识别
     * @param fileId 文件id
     * @return 识别结果
     */
    BillVO ocr(String fileId);
}
