package com.samoy.chuanbillserver.service;

public interface IVerificationCodeService {

    /**
     * 发送验证码
     *
     * @param phone 手机号
     */
    void sendCode(String phone);

    /**
     * 验证码验证
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 验证结果
     */
    boolean verifyCode(String phone, String code);
}
