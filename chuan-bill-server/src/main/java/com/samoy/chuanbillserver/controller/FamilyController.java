package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.CreateFamilyDTO;
import com.samoy.chuanbillserver.dto.HandleJoinApplyDTO;
import com.samoy.chuanbillserver.dto.JoinFamilyDTO;
import com.samoy.chuanbillserver.dto.RemoveMemberDTO;
import com.samoy.chuanbillserver.dto.TransferOwnerDTO;
import com.samoy.chuanbillserver.dto.UpdateFamilyDTO;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.service.IFamilyService;
import com.samoy.chuanbillserver.vo.BillVO;
import com.samoy.chuanbillserver.vo.FamilyJoinApplyVO;
import com.samoy.chuanbillserver.vo.FamilyMemberVO;
import com.samoy.chuanbillserver.vo.FamilyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 家庭控制器
 *
 * @author samoy
 * @since 2026/4/19
 */
@Tag(name = "family", description = "家庭相关接口")
@RestController
@RequestMapping("/family")
public class FamilyController {

    @Resource
    private IFamilyService familyService;

    @Resource
    private IBillService billService;

    @PostMapping("/create")
    @Operation(summary = "创建家庭", description = "创建一个新的家庭")
    public Result<FamilyVO> createFamily(@Validated @RequestBody CreateFamilyDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.createFamily(userId, dto));
    }

    @PostMapping("/update")
    @Operation(summary = "更新家庭信息", description = "更新家庭名称、头像、描述等信息")
    public Result<FamilyVO> updateFamily(@Validated @RequestBody UpdateFamilyDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.updateFamily(userId, dto));
    }

    @GetMapping("/detail")
    @Operation(summary = "获取家庭详情", description = "根据家庭ID获取家庭详细信息")
    public Result<FamilyVO> getFamilyDetail(
            @Parameter(description = "家庭ID", required = true) @RequestParam String familyId) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.getFamilyDetail(userId, familyId));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除家庭", description = "仅户主可删除家庭，删除后所有成员将被移除")
    public Result<Boolean> deleteFamily(
            @Parameter(description = "家庭ID", required = true) @RequestParam String familyId) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.deleteFamily(userId, familyId));
    }

    @GetMapping("/my-families")
    @Operation(summary = "获取我的家庭列表", description = "获取当前用户所在的所有家庭")
    public Result<List<FamilyVO>> getMyFamilies() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.getMyFamilies(userId));
    }

    @PostMapping("/join")
    @Operation(summary = "申请加入家庭", description = "通过邀请码申请加入家庭，需要户主审批")
    public Result<FamilyJoinApplyVO> joinFamily(@Validated @RequestBody JoinFamilyDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.joinFamily(userId, dto));
    }

    @PostMapping("/leave")
    @Operation(summary = "退出家庭", description = "退出当前所在的家庭，户主不能退出，需先转让户主身份")
    public Result<Boolean> leaveFamily(
            @Parameter(description = "家庭ID", required = true) @RequestParam String familyId) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.leaveFamily(userId, familyId));
    }

    @PostMapping("/remove-member")
    @Operation(summary = "移除家庭成员", description = "仅户主可移除家庭成员")
    public Result<Boolean> removeMember(@Validated @RequestBody RemoveMemberDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.removeMember(userId, dto));
    }

    @PostMapping("/transfer-owner")
    @Operation(summary = "转让户主", description = "将户主身份转让给其他家庭成员")
    public Result<Boolean> transferOwner(@Validated @RequestBody TransferOwnerDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.transferOwner(userId, dto));
    }

    @GetMapping("/members")
    @Operation(summary = "获取家庭成员列表", description = "获取指定家庭的成员列表")
    public Result<List<FamilyMemberVO>> getMembers(
            @Parameter(description = "家庭ID", required = true) @RequestParam String familyId) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.getMembers(userId, familyId));
    }

    @GetMapping("/pending-applies")
    @Operation(summary = "获取待处理的加入申请", description = "仅户主可查看待处理的加入申请列表")
    public Result<List<FamilyJoinApplyVO>> getPendingApplies(
            @Parameter(description = "家庭ID", required = true) @RequestParam String familyId) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.getPendingApplies(userId, familyId));
    }

    @PostMapping("/handle-apply")
    @Operation(summary = "处理加入申请", description = "户主同意或拒绝加入申请")
    public Result<Boolean> handleJoinApply(@Validated @RequestBody HandleJoinApplyDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.handleJoinApply(userId, dto));
    }

    @PostMapping("/refresh-invite-code")
    @Operation(summary = "刷新邀请码", description = "重新生成家庭邀请码，仅户主可操作")
    public Result<String> refreshInviteCode(
            @Parameter(description = "家庭ID", required = true) @RequestParam String familyId) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(familyService.refreshInviteCode(userId, familyId));
    }

    @GetMapping("/bills")
    @Operation(summary = "获取家庭账单列表", description = "分页获取家庭共享账单列表，仅家庭成员可查看")
    public Result<IPage<BillVO>> getFamilyBills(
            @Parameter(description = "家庭ID", required = true) @RequestParam String familyId,
            @Validated @ModelAttribute BillListDTO billListDTO) {
        String userId = StpUtil.getLoginIdAsString();
        // 检查用户是否是家庭成员
        if (!familyService.isMember(userId, familyId)) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }
        // 设置家庭ID并查询
        billListDTO.setFamilyId(familyId);
        return Result.success(billService.getBillListByPage(userId, billListDTO));
    }
}
