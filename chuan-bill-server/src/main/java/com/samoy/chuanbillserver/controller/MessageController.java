package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.samoy.chuanbillserver.dto.MessageListDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IMessageService;
import com.samoy.chuanbillserver.vo.MessageVO;
import com.samoy.chuanbillserver.vo.UnreadCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/message")
@Tag(name = "message", description = "消息通知相关接口")
public class MessageController {

    @Resource
    private IMessageService messageService;

    @GetMapping("/page-list")
    @Operation(summary = "获取消息列表", description = "分页获取消息列表，支持按类型和状态筛选")
    public Result<IPage<MessageVO>> getMessageList(@Validated @ModelAttribute MessageListDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(messageService.getMessageList(userId, dto));
    }

    @PostMapping("/mark-read")
    @Operation(summary = "标记消息已读", description = "将指定消息标记为已读")
    public Result<Boolean> markAsRead(@Parameter(description = "消息ID", required = true) @RequestParam String id) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(messageService.markAsRead(userId, id));
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "全部标记已读", description = "将所有未读消息标记为已读")
    public Result<Boolean> markAllAsRead() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(messageService.markAllAsRead(userId));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数量", description = "获取未读消息总数和家庭相关未读数")
    public Result<UnreadCountVO> getUnreadCount() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(messageService.getUnreadCount(userId));
    }
}
