package com.samoy.chuanbillserver.controller;

import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IAIService;
import com.samoy.chuanbillserver.vo.BillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@Tag(name = "ai", description = "ai 相关接口")
public class AIController {
    @Resource
    private IAIService aiService;

    @GetMapping("/ocr")
    @Operation(summary = "ocr", description = "ocr识别图片中的账单信息")
    public Result<BillVO> ocr(String fileId) {
        return Result.success(aiService.ocr(fileId));
    }
}
