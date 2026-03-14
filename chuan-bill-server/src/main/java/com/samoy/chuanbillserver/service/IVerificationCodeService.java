package com.samoy.chuanbillserver.service;

public interface IVerificationCodeService {

    void sendCode(String phone);

    boolean verifyCode(String phone, String code);
}
