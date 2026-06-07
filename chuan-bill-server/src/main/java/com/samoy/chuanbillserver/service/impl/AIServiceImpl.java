package com.samoy.chuanbillserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.google.gson.JsonObject;
import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.BillMonthlyStatsDTO;
import com.samoy.chuanbillserver.entity.AiSuggestion;
import com.samoy.chuanbillserver.entity.User;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.*;
import com.samoy.chuanbillserver.utils.AgentUtil;
import com.samoy.chuanbillserver.vo.AiAnalysisVO;
import com.samoy.chuanbillserver.vo.BillMonthlyStatsVO;
import com.samoy.chuanbillserver.vo.BillVO;
import jakarta.annotation.Resource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AIServiceImpl implements IAIService {

    @Resource
    private AgentUtil agentUtil;

    @Value("${dashscope.bill-recognition.app-id}")
    private String recognitionAppId;

    @Value("${dashscope.bill-analysis.app-id}")
    private String analysisAppId;

    @Resource
    private IBillService billService;

    @Resource
    private IFamilyService familyService;

    @Resource
    private IUserService userService;

    @Resource
    private IAiSuggestionService aiSuggestionService;

    @Resource
    private IAiUsageService aiUsageService;

    @Override
    public BillVO ocr(String fileId, String fileExt) {
        // 1. 通过fileId获取临时文件
        Path tempFile = Paths.get(SystemConstants.TEMP_FILE_UPLOAD_DIR, fileId);
        if (!PathUtil.exists(tempFile, false)) {
            throw new BusinessException(ResultEnum.FILE_NOT_FOUND);
        }
        Path tempPath = Paths.get(fileId + "." + fileExt);
        String mimeType = PathUtil.getMimeType(tempPath);
        // 如果不是图片，抛出异常
        if (!mimeType.startsWith("image")) {
            throw new BusinessException(ResultEnum.FILE_NOT_IMAGE);
        }
        // 2. 读取文件，将其转换为base64
        String imageBase64 = String.format("data:%s;base64,%s", mimeType, Base64.encode(PathUtil.readBytes(tempFile)));
        // 3. 调用OCR Agent
        try {
            ApplicationResult result = agentUtil.callAgent(recognitionAppId, "帮我提取账单信息", imageBase64);
            String output = result.getOutput().getText();
            JSON json = JSONUtil.parse(output);
            BillVO billVO = json.getByPath("ocrResult", BillVO.class);
            // 4. 识别后删除临时文件
            PathUtil.del(tempFile);
            if (billVO == null || billVO.getAmount() == null) {
                throw new BusinessException(ResultEnum.BILL_OCR_FAILED);
            }
            // 5. 返回识别结果
            return billVO;
        } catch (NoApiKeyException | InputRequiredException e) {
            throw new BusinessException(ResultEnum.BILL_OCR_FAILED);
        }
    }

    @Override
    public BillVO text(String text) {
        try {
            ApplicationResult result =
                    agentUtil.callAgent(recognitionAppId, String.format("[%s]，从上述文本中帮我提取账单信息", text));
            String output = result.getOutput().getText();
            JSON json = JSONUtil.parse(output);
            BillVO billVO = json.getByPath("nlpResult", BillVO.class);
            if (billVO == null || billVO.getAmount() == null) {
                throw new BusinessException(ResultEnum.BILL_TEXT_FAILED);
            }
            return billVO;
        } catch (NoApiKeyException | InputRequiredException e) {
            throw new BusinessException(ResultEnum.BILL_TEXT_FAILED);
        }
    }

    @Override
    public AiAnalysisVO analysis(Integer analysisType, String month, String familyId, boolean regenerate) {
        String userId = StpUtil.getLoginIdAsString();
        User user = userService.getById(userId);
        boolean isVip = Boolean.TRUE.equals(user.getIsVip());

        // 非重新生成时，优先返回缓存
        if (!regenerate) {
            AiSuggestion suggestion = aiSuggestionService.getByUserIdAndMonth(analysisType, userId, familyId, month);
            AiAnalysisVO vo = new AiAnalysisVO();
            if (suggestion != null) {
                vo.setContent(suggestion.getContent());
            }
            vo.setCached(true);
            vo.setRemainingCount(aiUsageService.getRemainingCount(userId, isVip));
            return vo;
        }

        // 需要调用AI生成（无缓存或重新生成）
        int remainingCount = aiUsageService.getRemainingCount(userId, isVip);
        if (remainingCount <= 0 && !isVip) {
            throw new BusinessException(ResultEnum.AI_ANALYSIS_RATE_LIMITED);
        }

        // 调用DashScope生成分析
        String analysisText = generateAnalysis(analysisType, userId, month, familyId);

        // 持久化建议
        aiSuggestionService.saveOrUpdateSuggestion(analysisType, userId, familyId, month, analysisText);

        // 递增每日使用次数（VIP也需要记录，但不受限制）
        aiUsageService.incrementUsage(userId);

        AiAnalysisVO vo = new AiAnalysisVO();
        vo.setContent(analysisText);
        vo.setCached(false);
        vo.setRemainingCount(aiUsageService.getRemainingCount(userId, isVip));
        return vo;
    }

    /**
     * 调用DashScope生成账单分析
     *
     * @param userId 用户ID
     * @param month  月份
     * @return 分析文本
     */
    private String generateAnalysis(Integer analysisType, String userId, String month, String familyId) {
        BillMonthlyStatsVO vo = billService.getMonthlyStats(userId, new BillMonthlyStatsDTO(month, familyId));
        BillListDTO billListDTO = new BillListDTO();
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        String startDate = yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = yearMonth.atEndOfMonth().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        billListDTO.setStartDate(startDate);
        billListDTO.setEndDate(endDate);
        if (analysisType == SystemConstants.FAMILY_ANALYSIS_TYPE) {
            if (!familyService.isOwner(userId, familyId)) {
                throw new BusinessException(ResultEnum.FAMILY_NOT_OWNER);
            }
            billListDTO.setFamilyId(familyId);
        }
        List<BillVO> billVOList = billService.getBillList(userId, billListDTO);
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下账单数据：\n");
        sb.append(String.format("【%s收入】：%s%n", month, vo.getIncome()));
        sb.append(String.format("【%s支出】：%s%n", month, vo.getExpense()));
        sb.append(String.format("【%s结余】：%s%n", month, vo.getBalance()));
        sb.append(String.format("【%s账单明细】%n%s", month, JSONUtil.toJsonPrettyStr(billVOList)));
        log.debug("查询语句：\n{}", sb);
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("analysisType", analysisType);
            ApplicationResult result = agentUtil.callAgent(analysisAppId, sb.toString(), jsonObject);
            return result.getOutput().getText();
        } catch (NoApiKeyException | InputRequiredException e) {
            throw new BusinessException(ResultEnum.BILL_ANALYSIS_FAILED);
        }
    }
}
