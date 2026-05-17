package com.samoy.chuanbillserver.service.impl;

import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.enums.SmsScene;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IVerificationCodeService;
import com.samoy.chuanbillserver.utils.SmsUtil;
import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VerificationCodeServiceImpl implements IVerificationCodeService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private SmsUtil smsUtil;

    @Value("${spring.profiles.active}")
    private String env;

    @Override
    public void sendCode(SmsScene smsScene, String phone) {
        String code = generateCode();
        // 如果是开发测试环境，使用以下方式生成验证码
        if ("dev".equals(env)) {
            log.info("向手机号 {} 发送验证码 {}", phone, code);
        }
        if ("prod".equals(env)) {
            try {
                smsUtil.sendSms(smsScene, phone, code);
            } catch (Exception e) {
                log.error("发送短信验证码失败，场景：{}, 手机号：{}，验证码：{}", smsScene.getScene(), phone, code, e);
                throw new BusinessException(ResultEnum.SMS_SEND_FAILED);
            }
        }

        // 保存验证码到Redis
        String key = SystemConstants.CODE_CACHE_KEY_PREFIX + phone;
        redisTemplate.opsForValue().set(key, code, SystemConstants.CODE_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String key = SystemConstants.CODE_CACHE_KEY_PREFIX + phone;
        String cachedCode = redisTemplate.opsForValue().get(key);

        if (cachedCode == null) {
            throw new BusinessException(ResultEnum.TOKEN_EXPIRED);
        }

        if (!cachedCode.equals(code)) {
            return false;
        }

        // 验证成功后删除缓存的验证码
        redisTemplate.delete(key);

        return true;
    }

    /**
     * 生成验证码
     *
     * @return 验证码
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SystemConstants.CODE_LENGTH; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }
}
