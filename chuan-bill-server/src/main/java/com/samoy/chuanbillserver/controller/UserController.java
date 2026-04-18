package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.dto.UpdatePasswordByCodeDTO;
import com.samoy.chuanbillserver.dto.UpdatePasswordByOldDTO;
import com.samoy.chuanbillserver.dto.UserProfileUpdateDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IUserService;
import com.samoy.chuanbillserver.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user")
@RestController
@Tag(name = "user", description = "用户信息、密码修改等相关接口")
public class UserController {

    @Resource
    private IUserService userService;

    @GetMapping("/profile")
    @Operation(summary = "获取用户资料", description = "获取当前登录用户的详细信息")
    public Result<UserVO> getProfile() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(userService.getProfileById(userId));
    }

    @PostMapping("/profile/update")
    @Operation(summary = "更新用户资料", description = "更新用户的昵称、头像、性别等信息")
    public Result<Boolean> updateProfile(@Validated @RequestBody UserProfileUpdateDTO updateDTO) {
        String userId = StpUtil.getLoginIdAsString();
        updateDTO.setUserId(userId);
        return Result.success(userService.updateUserProfile(updateDTO));
    }

    @PostMapping("/password/update-by-old")
    @Operation(summary = "通过旧密码修改密码", description = "使用原密码设置新密码")
    public Result<Boolean> updatePasswordByOld(@Validated @RequestBody UpdatePasswordByOldDTO updateDTO) {
        String userId = StpUtil.getLoginIdAsString();
        updateDTO.setUserId(userId);
        return Result.success(userService.updatePassWordByOld(updateDTO));
    }

    @PostMapping("/password/update-by-code")
    @SaIgnore
    @Operation(summary = "通过验证码修改密码", description = "使用手机验证码设置新密码（无需登录）")
    public Result<Boolean> updatePasswordByCode(@Validated @RequestBody UpdatePasswordByCodeDTO updateDTO) {
        return Result.success(userService.updatePassWordByCode(updateDTO));
    }

    @GetMapping("/has-password")
    @Operation(summary = "检查是否设置了密码", description = "检查当前用户是否已设置登录密码")
    public Result<Boolean> hasPassword() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(userService.hasPassword(userId));
    }
}
