package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.dto.AddBillDTO;
import com.samoy.chuanbillserver.expection.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IAIService;
import com.samoy.chuanbillserver.utils.OCRUtil;
import jakarta.annotation.Resource;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;

@Service
public class AIServiceImpl implements IAIService {

    @Resource
    private OCRUtil ocrUtil;

    @Override
    public AddBillDTO ocr(String fileId) {
        // 1. 通过fileId获取临时文件
        Path tempFile = Paths.get(SystemConstants.TEMP_FILE_UPLOAD_DIR, fileId);
        if (!FileUtil.exists(tempFile, false)) {
            throw new BusinessException(ResultEnum.FILE_NOT_FOUND);
        }
        // 2. 读取文件，将其转换为base64
        String imageBase64 = String.format(
                "data:%s;base64,%s", FileUtil.getMimeType(tempFile), Base64.encode(FileUtil.readBytes(tempFile)));
        // 3. 调用OCR Agent
        try {
            ApplicationResult result = ocrUtil.callAgent("帮我提取账单", imageBase64);
            String output = result.getOutput().getText();
            JSON json = JSONUtil.parse(output);
            AddBillDTO addBillDTO = json.getByPath("result", AddBillDTO.class);
            // 4. 识别成功删除临时文件
            FileUtil.del(tempFile);
            // 5. 返回识别结果
            return addBillDTO;
        } catch (NoApiKeyException | InputRequiredException e) {
            throw new BusinessException(ResultEnum.BILL_OCR_FAILED);
        }
    }
}
