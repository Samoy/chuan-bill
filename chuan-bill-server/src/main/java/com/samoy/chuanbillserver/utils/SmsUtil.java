package com.samoy.chuanbillserver.utils;

import com.aliyun.auth.credentials.provider.DefaultCredentialProvider;
import com.aliyun.auth.credentials.provider.EnvironmentVariableCredentialProvider;
import com.aliyun.auth.credentials.provider.SystemPropertiesCredentialProvider;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.google.gson.Gson;
import com.samoy.chuanbillserver.enums.SmsScene;
import darabonba.core.client.ClientOverrideConfiguration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 短信工具类
 * </p>
 *
 * @author samoy
 * @since 2026/5/16
 */
@Component
public class SmsUtil {

    public void sendSms(SmsScene smsScene, String phone, String code) throws ExecutionException, InterruptedException {
        DefaultCredentialProvider provider = DefaultCredentialProvider.builder()
                .customizeProviders(
                        SystemPropertiesCredentialProvider.create(), EnvironmentVariableCredentialProvider.create())
                .build();

        try (AsyncClient client = AsyncClient.builder()
                .region("cn-hangzhou")
                .credentialsProvider(provider)
                .overrideConfiguration(
                        ClientOverrideConfiguration.create().setEndpointOverride("dypnsapi.aliyuncs.com"))
                .build()) {

            SendSmsVerifyCodeRequest sendSmsVerifyCodeRequest = SendSmsVerifyCodeRequest.builder()
                    .phoneNumber(phone)
                    .signName("速通互联验证码")
                    .templateCode(smsScene.getTemplateCode())
                    .templateParam("{\"code\":\"" + code + "\",\"min\":\"5\"}")
                    .codeLength((long) code.length())
                    .build();

            CompletableFuture<SendSmsVerifyCodeResponse> response = client.sendSmsVerifyCode(sendSmsVerifyCodeRequest);
            SendSmsVerifyCodeResponse resp = response.get();
            System.out.println(new Gson().toJson(resp));
        }
    }
}
