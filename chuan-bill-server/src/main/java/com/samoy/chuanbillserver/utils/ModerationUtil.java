package com.samoy.chuanbillserver.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.aliyun.auth.credentials.provider.DefaultCredentialProvider;
import com.aliyun.auth.credentials.provider.EnvironmentVariableCredentialProvider;
import com.aliyun.auth.credentials.provider.SystemPropertiesCredentialProvider;
import com.aliyun.sdk.service.green20220302.AsyncClient;
import com.aliyun.sdk.service.green20220302.models.*;
import com.samoy.chuanbillserver.enums.ModerationScene;
import darabonba.core.client.ClientOverrideConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 内容合规工具
 * </p>
 *
 * @author samoy
 * @since 2026/6/13
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationUtil {

    private final StringRedisTemplate redisTemplate;

    private AsyncClient greenClient;

    /**
     * 缓存key前缀
     */
    private static final String CACHE_PREFIX = "content:moderation:text:";

    /**
     * 缓存过期时间：14天
     */
    private static final Duration CACHE_TTL = Duration.ofDays(14);

    @PostConstruct
    public void init() {
        DefaultCredentialProvider provider = DefaultCredentialProvider.builder()
                .customizeProviders(
                        SystemPropertiesCredentialProvider.create(), EnvironmentVariableCredentialProvider.create())
                .build();
        greenClient = AsyncClient.builder()
                .region("cn-shanghai")
                .credentialsProvider(provider)
                .overrideConfiguration(ClientOverrideConfiguration.create()
                        // Endpoint 请参考 https://api.aliyun.com/product/Green
                        .setEndpointOverride("green-cip.cn-shanghai.aliyuncs.com"))
                .build();
    }

    /**
     * 检查文本是否符合规范
     *
     * @param text  文本
     * @param scene 场景
     * @return 是否符合规范
     */
    public boolean checkText(String text, ModerationScene scene) {
        if (!StrUtil.isNotEmpty(text)) {
            return true;
        }

        String cacheKey = CACHE_PREFIX + SecureUtil.md5(text);

        // 1. 检查缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("命中缓存: key={}", cacheKey);
            return Boolean.parseBoolean(cached);
        }

        // 2. 调用阿里云API
        boolean passed = callAliyunApi(text, scene.getService());

        // 3. 存入缓存
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(passed), CACHE_TTL);
        log.debug("缓存写入: key={}, passed={}", cacheKey, passed);

        return passed;
    }

    private boolean callAliyunApi(String text, String service) {
        try {
            Map<String, Object> serviceParameters = new HashMap<>();
            serviceParameters.put("content", text);

            TextModerationPlusRequest request = TextModerationPlusRequest.builder()
                    .service(service)
                    .serviceParameters(JSONUtil.toJsonStr(serviceParameters))
                    .build();
            CompletableFuture<TextModerationPlusResponse> res = greenClient.textModerationPlus(request);
            TextModerationPlusResponse response = res.get();

            if (response.getBody() == null) {
                log.warn("阿里云内容安全响应为空");
                return true;
            }

            TextModerationPlusResponseBody body = response.getBody();

            // 检查响应码
            if (ObjectUtil.equals(body, 200)) {
                log.warn("阿里云内容安全调用失败: code={}, message={}", body.getCode(), body.getMessage());
                return true;
            }
            // 检查审核结果
            if (body.getData() != null) {
                log.debug("阿里云内容安全审核结果: {}", body.getData());
                String riskLevel = body.getData().getRiskLevel();
                // 高风险和中风险不通过，低风险和未检测到风险通过
                return !"high".equals(riskLevel) && !"medium".equals(riskLevel);
            }

            return true;
        } catch (Exception e) {
            log.error("调用阿里云内容安全服务异常", e);
            // 异常时放行，避免阻塞正常业务
            return true;
        }
    }

    @PreDestroy
    public void destroy() {
        if (greenClient != null) {
            greenClient = null;
        }
    }
}
