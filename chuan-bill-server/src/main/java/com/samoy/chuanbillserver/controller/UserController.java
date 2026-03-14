package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.dto.UpdatePasswordByCodeDTO;
import com.samoy.chuanbillserver.dto.UpdatePasswordByOldDTO;
import com.samoy.chuanbillserver.dto.UserProfileUpdateDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IUserService;
import com.samoy.chuanbillserver.service.IVerificationCodeService;
import com.samoy.chuanbillserver.vo.UserVO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user")
@RestController
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IVerificationCodeService verificationCodeService;

    @GetMapping("/profile")
    public Result<UserVO> getProfile() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(userService.getProfileById(userId));
    }

    @PostMapping("/updateProfile")
    public Result<Boolean> updateProfile(@Validated @RequestBody UserProfileUpdateDTO updateDTO) {
        String userId = StpUtil.getLoginIdAsString();
        updateDTO.setUserId(userId);
        return Result.success(userService.updateUserProfile(updateDTO));
    }

    @PostMapping("/updatePasswordByOld")
    public Result<Boolean> updatePasswordByOld(@Validated @RequestBody UpdatePasswordByOldDTO updateDTO) {
        String userId = StpUtil.getLoginIdAsString();
        updateDTO.setUserId(userId);
        return Result.success(userService.updatePassWordByOld(updateDTO));
    }

    @PostMapping("/updatePasswordByCode")
    @SaIgnore
    public Result<Boolean> updatePasswordByCode(@Validated @RequestBody UpdatePasswordByCodeDTO updateDTO) {
        return Result.success(userService.updatePassWordByCode(updateDTO));
    }

    @GetMapping("/hasPassword")
    public Result<Boolean> hasPassword() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(userService.hasPassword(userId));
    }
}
