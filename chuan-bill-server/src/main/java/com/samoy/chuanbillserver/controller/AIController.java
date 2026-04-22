package com.samoy.chuanbillserver.controller;

import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IAIService;
import com.samoy.chuanbillserver.vo.AiAnalysisVO;
import com.samoy.chuanbillserver.vo.BillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@Tag(name = "ai", description = "ai 相关接口")
public class AIController {
    @Resource
    private IAIService aiService;

    @GetMapping("/ocr")
    @Operation(summary = "ocr识别", description = "ocr识别图片中的账单信息")
    public Result<BillVO> ocr(String fileId, String fileExt) {
        return Result.success(aiService.ocr(fileId, fileExt));
    }

    @GetMapping("/text")
    @Operation(summary = "文本识别", description = "识别文本中的账单信息")
    public Result<BillVO> text(String text) {
        return Result.success(aiService.text(text));
    }

    @GetMapping("/analysis")
    @Operation(summary = "分析", description = "分析账单信息，优先返回缓存结果，regenerate=true时重新生成")
    public Result<AiAnalysisVO> analysis(
            Integer analysisType,
            @Validated @Pattern(regexp = "^\\d{4}-\\d{2}$") String month,
            @RequestParam(required = false) String familyId,
            @RequestParam(defaultValue = "false") boolean regenerate) {
        return Result.success(aiService.analysis(analysisType, month, familyId, regenerate));
    }
}
