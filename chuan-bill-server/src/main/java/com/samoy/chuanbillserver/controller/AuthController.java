package com.samoy.chuanbillserver.controller;

import com.samoy.chuanbillserver.dto.*;
import com.samoy.chuanbillserver.enums.SmsScene;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IUserService;
import com.samoy.chuanbillserver.service.IVerificationCodeService;
import com.samoy.chuanbillserver.vo.TokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "auth", description = "认证、验证码相关接口")
public class AuthController {
    @Resource
    private IUserService userService;

    @Resource
    private IVerificationCodeService verificationCodeService;

    /**
     * 密码登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @PostMapping("/login-password")
    @Operation(summary = "密码登录", description = "使用手机号和密码进行登录")
    public Result<TokenVO> loginByPassword(@Validated @RequestBody LoginByPasswordDTO loginDTO) {
        return Result.success(userService.loginByPassword(loginDTO));
    }

    /**
     * 手机号登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @PostMapping("/login-phone")
    @Operation(summary = "手机号登录", description = "使用手机号和验证码进行登录")
    public Result<TokenVO> loginByPhone(@Validated @RequestBody LoginByPhoneDTO loginDTO) {
        return Result.success(userService.loginByPhone(loginDTO));
    }

    /**
     * 微信登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @PostMapping("/login-wechat")
    @Operation(summary = "微信登录", description = "使用微信小程序 code 进行登录")
    public Result<TokenVO> loginByWechat(@Validated @RequestBody LoginByWechatDTO loginDTO) {
        return Result.success(userService.loginByWechat(loginDTO));
    }

    /**
     * 发送验证码
     *
     * @param sendCodeDTO 发送验证码请求体，包含手机号
     * @return 发送结果
     */
    @PostMapping("/send-code")
    @Operation(summary = "发送验证码", description = "向指定手机号发送短信验证码")
    public Result<Void> sendCode(@Validated @RequestBody SendCodeDTO sendCodeDTO) {
        verificationCodeService.sendCode(SmsScene.LOGIN, sendCodeDTO.getPhone());
        return Result.success();
    }

    /**
     * 找回密码
     */
    @PostMapping("/retrieve-password")
    @Operation(summary = "找回密码", description = "使用手机号和验证码进行密码重置")
    public Result<Boolean> retrievePassword(@Validated @RequestBody RetrievePasswordDTO retrievePasswordDTO) {
        return Result.success(userService.retrievePassword(retrievePasswordDTO));
    }

    /**
     * 登出
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "登出", description = "用户登出，清除登录信息")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }
}
