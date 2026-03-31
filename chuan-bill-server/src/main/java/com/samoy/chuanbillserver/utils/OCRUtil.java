package com.samoy.chuanbillserver.utils;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OCRUtil {
    @Value("${dashscope.apiKey}")
    private String dashscopeApiKey;

    @Value("${dashscope.ocr.appId}")
    private String ocrAppId;

    public ApplicationResult callAgent(String query, String imageBase64)
            throws NoApiKeyException, InputRequiredException {
        log.info("apiKey: {}", dashscopeApiKey);
        log.info("appId: {}", ocrAppId);
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(dashscopeApiKey)
                .appId(ocrAppId)
                .prompt(query)
                .hasThoughts(true)
                .images(Collections.singletonList(imageBase64))
                .build();
        Application application = new Application();
        return application.call(param);
    }
}
