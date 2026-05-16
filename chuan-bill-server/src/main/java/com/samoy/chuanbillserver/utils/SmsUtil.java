package com.samoy.chuanbillserver.utils;

import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    void init() {}

    public void sendSms(String phone, String code) {
        System.out.println("发送短信：" + phone + " " + code);
    }
}
