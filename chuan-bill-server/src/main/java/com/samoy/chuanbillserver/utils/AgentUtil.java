package com.samoy.chuanbillserver.utils;

import cn.hutool.core.util.ObjectUtil;
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
public class AgentUtil {
    @Value("${dashscope.api-key}")
    private String dashscopeApiKey;

    public ApplicationResult callAgent(String appId, String query) throws NoApiKeyException, InputRequiredException {
        return callAgent(appId, query, null);
    }

    public ApplicationResult callAgent(String appId, String query, String imageBase64)
            throws NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(dashscopeApiKey)
                .appId(appId)
                .prompt(query)
                .hasThoughts(true)
                .build();
        if (ObjectUtil.isNotEmpty(imageBase64)) {
            param.setImages(Collections.singletonList(imageBase64));
        }
        Application application = new Application();
        return application.call(param);
    }
}
