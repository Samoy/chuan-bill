package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.dto.PreferenceSetDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * 用户偏好设置 Controller
 *
 * @author Samoy
 * @since 2026-05-12
 */
@RestController
@RequestMapping("/preference")
@Tag(name = "preference", description = "用户偏好设置相关接口")
public class UserPreferenceController {

    @Resource
    private IUserPreferenceService userPreferenceService;

    @GetMapping("/get")
    @Operation(summary = "获取单个偏好", description = "根据键名获取单个偏好值")
    public Result<String> get(@Parameter(description = "偏好键名", required = true) @RequestParam String key) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(userPreferenceService.getValue(userId, key));
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有偏好", description = "获取用户所有偏好设置")
    public Result<Map<String, String>> getAll() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(userPreferenceService.getAll(userId));
    }

    @PostMapping("/set")
    @Operation(summary = "设置偏好", description = "设置单个偏好值")
    public Result<Boolean> set(@RequestBody @Valid PreferenceSetDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        userPreferenceService.setValue(userId, dto.getKey(), dto.getValue());
        return Result.success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除偏好", description = "删除单个偏好设置")
    public Result<Boolean> delete(@Parameter(description = "偏好键名", required = true) @RequestParam String key) {
        String userId = StpUtil.getLoginIdAsString();
        userPreferenceService.deleteValue(userId, key);
        return Result.success(true);
    }
}
