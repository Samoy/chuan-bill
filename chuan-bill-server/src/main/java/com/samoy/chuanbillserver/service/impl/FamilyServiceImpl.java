package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.dao.FamilyMapper;
import com.samoy.chuanbillserver.dto.CreateFamilyDTO;
import com.samoy.chuanbillserver.dto.HandleJoinApplyDTO;
import com.samoy.chuanbillserver.dto.JoinFamilyDTO;
import com.samoy.chuanbillserver.dto.RemoveMemberDTO;
import com.samoy.chuanbillserver.dto.TransferOwnerDTO;
import com.samoy.chuanbillserver.dto.UpdateFamilyDTO;
import com.samoy.chuanbillserver.entity.Family;
import com.samoy.chuanbillserver.entity.FamilyJoinApply;
import com.samoy.chuanbillserver.entity.FamilyMember;
import com.samoy.chuanbillserver.entity.User;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IFamilyJoinApplyService;
import com.samoy.chuanbillserver.service.IFamilyMemberService;
import com.samoy.chuanbillserver.service.IFamilyService;
import com.samoy.chuanbillserver.service.IMessageService;
import com.samoy.chuanbillserver.service.IUserService;
import com.samoy.chuanbillserver.vo.FamilyJoinApplyVO;
import com.samoy.chuanbillserver.vo.FamilyMemberVO;
import com.samoy.chuanbillserver.vo.FamilyVO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 家庭表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Service
public class FamilyServiceImpl extends ServiceImpl<FamilyMapper, Family> implements IFamilyService {

    @Resource
    private IFamilyMemberService familyMemberService;

    @Resource
    private IFamilyJoinApplyService familyJoinApplyService;

    @Resource
    private IUserService userService;

    @Resource
    private IMessageService messageService;

    private static final int INVITE_CODE_LENGTH = 6;

    private static final int MAX_CREATED_FAMILIES = 5;

    @Override
    @Transactional
    public FamilyVO createFamily(String userId, CreateFamilyDTO dto) {
        // 检查用户创建的家庭数量是否已达上限
        long createdCount = familyMemberService.count(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getUserId, userId)
                .eq(FamilyMember::getIsOwner, true));
        if (createdCount >= MAX_CREATED_FAMILIES) {
            throw new BusinessException(ResultEnum.FAMILY_CREATE_LIMIT_REACHED);
        }

        Family family = new Family();
        family.setName(dto.getName());
        family.setAvatar(dto.getAvatar());
        family.setDescription(dto.getDescription());
        family.setOwnerId(userId);
        family.setInviteCode(generateInviteCode());
        this.save(family);

        // 创建者自动成为户主成员
        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(userId);
        member.setNickname(userService.getById(userId).getNickname());
        member.setIsOwner(true);
        member.setJoinTime(LocalDateTime.now());
        familyMemberService.save(member);

        return convertToFamilyVO(family, userId);
    }

    @Override
    public FamilyVO updateFamily(String userId, UpdateFamilyDTO dto) {
        Family family = this.getById(dto.getId());
        if (family == null) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_FOUND);
        }
        if (!isOwner(userId, family.getId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_OWNER);
        }
        if (dto.getName() != null) {
            family.setName(dto.getName());
        }
        if (dto.getAvatar() != null) {
            family.setAvatar(dto.getAvatar());
        }
        if (dto.getDescription() != null) {
            family.setDescription(dto.getDescription());
        }
        this.updateById(family);
        return convertToFamilyVO(family, userId);
    }

    @Override
    public FamilyVO getFamilyDetail(String userId, String familyId) {
        Family family = this.getById(familyId);
        if (family == null) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_FOUND);
        }
        if (!isMember(userId, familyId)) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }
        return convertToFamilyVO(family, userId);
    }

    @Override
    @Transactional
    public boolean deleteFamily(String userId, String familyId) {
        Family family = this.getById(familyId);
        if (family == null) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_FOUND);
        }
        if (!isOwner(userId, familyId)) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_OWNER);
        }
        // 删除所有成员
        familyMemberService.remove(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId, familyId));
        // 删除所有申请
        familyJoinApplyService.remove(
                new LambdaQueryWrapper<FamilyJoinApply>().eq(FamilyJoinApply::getFamilyId, familyId));
        // 删除家庭
        return this.removeById(familyId);
    }

    @Override
    public List<FamilyVO> getMyFamilies(String userId) {
        List<FamilyMember> members =
                familyMemberService.list(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getUserId, userId));
        if (CollUtil.isEmpty(members)) {
            return Collections.emptyList();
        }
        List<String> familyIds = members.stream().map(FamilyMember::getFamilyId).toList();
        List<Family> families = this.listByIds(familyIds);
        return families.stream().map(f -> convertToFamilyVO(f, userId)).toList();
    }

    @Override
    @Transactional
    public FamilyJoinApplyVO joinFamily(String userId, JoinFamilyDTO dto) {
        // 根据邀请码查找家庭
        Family family = this.getOne(new LambdaQueryWrapper<Family>().eq(Family::getInviteCode, dto.getInviteCode()));
        if (family == null) {
            throw new BusinessException(ResultEnum.FAMILY_INVITE_CODE_INVALID);
        }
        // 检查是否已是成员
        if (isMember(userId, family.getId())) {
            throw new BusinessException(ResultEnum.FAMILY_ALREADY_MEMBER);
        }
        // 检查是否已有待处理申请
        long pendingCount = familyJoinApplyService.count(new LambdaQueryWrapper<FamilyJoinApply>()
                .eq(FamilyJoinApply::getFamilyId, family.getId())
                .eq(FamilyJoinApply::getUserId, userId)
                .eq(FamilyJoinApply::getStatus, 0));
        if (pendingCount > 0) {
            throw new BusinessException(ResultEnum.FAMILY_APPLY_ALREADY_PENDING);
        }
        // 创建申请
        FamilyJoinApply apply = new FamilyJoinApply();
        apply.setFamilyId(family.getId());
        apply.setUserId(userId);
        apply.setRemark(dto.getRemark());
        apply.setStatus(0);
        apply.setHandleUserId(family.getOwnerId());
        familyJoinApplyService.save(apply);

        // 通知户主
        messageService.sendMessage(
                family.getOwnerId(),
                "新成员申请加入",
                "有用户申请加入您的家庭「" + family.getName() + "」",
                "family",
                family.getId(),
                "family");

        return convertToApplyVO(apply);
    }

    @Override
    @Transactional
    public boolean leaveFamily(String userId, String familyId) {
        Family family = this.getById(familyId);
        if (family == null) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_FOUND);
        }
        if (isOwner(userId, familyId)) {
            throw new BusinessException(ResultEnum.FAMILY_OWNER_CANNOT_LEAVE);
        }
        return familyMemberService.remove(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, userId));
    }

    @Override
    @Transactional
    public boolean removeMember(String userId, RemoveMemberDTO dto) {
        if (!isOwner(userId, dto.getFamilyId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_OWNER);
        }
        FamilyMember targetMember = familyMemberService.getOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getId, dto.getMemberId())
                .eq(FamilyMember::getFamilyId, dto.getFamilyId()));
        if (targetMember == null) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }
        if (targetMember.getIsOwner()) {
            throw new BusinessException(ResultEnum.FAMILY_CANNOT_REMOVE_OWNER);
        }
        return familyMemberService.removeById(dto.getMemberId());
    }

    @Override
    @Transactional
    public boolean transferOwner(String userId, TransferOwnerDTO dto) {
        Family family = this.getById(dto.getFamilyId());
        if (family == null) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_FOUND);
        }
        if (!isOwner(userId, dto.getFamilyId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_OWNER);
        }
        // 检查目标用户是否是家庭成员
        FamilyMember targetMember = familyMemberService.getOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, dto.getFamilyId())
                .eq(FamilyMember::getUserId, dto.getTargetUserId()));
        if (targetMember == null) {
            throw new BusinessException(ResultEnum.FAMILY_TRANSFER_TARGET_NOT_MEMBER);
        }
        // 当前户主改为普通成员
        FamilyMember currentOwner = familyMemberService.getOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, dto.getFamilyId())
                .eq(FamilyMember::getUserId, userId));
        currentOwner.setIsOwner(false);
        familyMemberService.updateById(currentOwner);

        // 目标用户改为户主
        targetMember.setIsOwner(true);
        familyMemberService.updateById(targetMember);

        // 更新家庭表的 ownerId
        family.setOwnerId(dto.getTargetUserId());
        this.updateById(family);

        // 通知新户主
        messageService.sendMessage(
                dto.getTargetUserId(),
                "您已成为户主",
                "您已被转让为家庭「" + family.getName() + "」的户主",
                "family",
                family.getId(),
                "family");

        return true;
    }

    @Override
    public List<FamilyMemberVO> getMembers(String userId, String familyId) {
        if (!isMember(userId, familyId)) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }
        List<FamilyMember> members = familyMemberService.list(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .orderByDesc(FamilyMember::getIsOwner)
                .orderByAsc(FamilyMember::getJoinTime));
        if (CollUtil.isEmpty(members)) {
            return Collections.emptyList();
        }
        // 批量查询用户信息，避免 N+1
        List<String> userIds =
                members.stream().map(FamilyMember::getUserId).distinct().toList();
        Map<String, User> userMap = batchQueryUsers(userIds);
        return members.stream().map(m -> convertToMemberVO(m, userMap)).toList();
    }

    @Override
    public List<FamilyJoinApplyVO> getPendingApplies(String userId, String familyId) {
        if (!isOwner(userId, familyId)) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_OWNER);
        }
        List<FamilyJoinApply> applies = familyJoinApplyService.list(new LambdaQueryWrapper<FamilyJoinApply>()
                .eq(FamilyJoinApply::getFamilyId, familyId)
                .eq(FamilyJoinApply::getStatus, 0)
                .orderByDesc(FamilyJoinApply::getCreateTime));
        if (CollUtil.isEmpty(applies)) {
            return Collections.emptyList();
        }
        // 批量查询用户信息
        List<String> userIds =
                applies.stream().map(FamilyJoinApply::getUserId).distinct().toList();
        Map<String, User> userMap = batchQueryUsers(userIds);
        return applies.stream().map(a -> convertToApplyVO(a, userMap)).toList();
    }

    @Override
    @Transactional
    public boolean handleJoinApply(String userId, HandleJoinApplyDTO dto) {
        FamilyJoinApply apply = familyJoinApplyService.getById(dto.getApplyId());
        if (apply == null) {
            throw new BusinessException(ResultEnum.FAMILY_APPLY_NOT_FOUND);
        }
        if (apply.getStatus() != 0) {
            throw new BusinessException(ResultEnum.FAMILY_APPLY_ALREADY_HANDLED);
        }
        if (!isOwner(userId, apply.getFamilyId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_OWNER);
        }
        apply.setStatus(
                dto.getApproved()
                        ? SystemConstants.AGREE_FAMILY_MEMBER_APPLY
                        : SystemConstants.REFUSE_FAMILY_MEMBER_APPLY);
        apply.setHandleUserId(userId);
        apply.setHandleTime(LocalDateTime.now());
        familyJoinApplyService.updateById(apply);

        if (dto.getApproved()) {
            // 同意申请：添加为家庭成员
            FamilyMember member = new FamilyMember();
            member.setFamilyId(apply.getFamilyId());
            member.setUserId(apply.getUserId());
            member.setIsOwner(false);
            member.setJoinTime(LocalDateTime.now());
            member.setNickname(userService.getById(apply.getUserId()).getNickname());
            familyMemberService.save(member);

            // 通知申请人
            Family family = this.getById(apply.getFamilyId());
            messageService.sendMessage(
                    apply.getUserId(),
                    "申请已通过",
                    "您已成功加入家庭「" + (family != null ? family.getName() : "") + "」",
                    "family",
                    apply.getFamilyId(),
                    "family");
        } else {
            // 通知申请人被拒绝
            Family family = this.getById(apply.getFamilyId());
            messageService.sendMessage(
                    apply.getUserId(),
                    "申请被拒绝",
                    "您加入家庭「" + (family != null ? family.getName() : "") + "」的申请被拒绝",
                    "family",
                    apply.getFamilyId(),
                    "family");
        }
        return true;
    }

    @Override
    public String refreshInviteCode(String userId, String familyId) {
        if (!isOwner(userId, familyId)) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_OWNER);
        }
        Family family = this.getById(familyId);
        if (family == null) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_FOUND);
        }
        String newCode = generateInviteCode();
        family.setInviteCode(newCode);
        this.updateById(family);
        return newCode;
    }

    @Override
    public boolean isMember(String userId, String familyId) {
        return familyMemberService.count(new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
                        .eq(FamilyMember::getUserId, userId))
                > 0;
    }

    @Override
    public boolean isOwner(String userId, String familyId) {
        return familyMemberService.count(new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
                        .eq(FamilyMember::getUserId, userId)
                        .eq(FamilyMember::getIsOwner, true))
                > 0;
    }

    // ========== 私有方法 ==========

    private String generateInviteCode() {
        return RandomUtil.randomNumbers(INVITE_CODE_LENGTH);
    }

    private FamilyVO convertToFamilyVO(Family family, String userId) {
        FamilyVO vo = new FamilyVO();
        vo.setId(family.getId());
        vo.setName(family.getName());
        vo.setAvatar(family.getAvatar());
        vo.setDescription(family.getDescription());
        vo.setOwnerId(family.getOwnerId());
        vo.setInviteCode(family.getInviteCode());
        vo.setCreateTime(family.getCreateTime());

        // 查询户主信息
        User owner = userService.getById(family.getOwnerId());
        if (owner != null) {
            vo.setOwnerNickname(owner.getNickname());
            vo.setOwnerAvatar(owner.getAvatar());
        }

        // 成员数量
        long memberCount = familyMemberService.count(
                new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId, family.getId()));
        vo.setMemberCount((int) memberCount);

        // 是否是户主
        vo.setIsOwner(Objects.equals(family.getOwnerId(), userId));

        return vo;
    }

    private FamilyMemberVO convertToMemberVO(FamilyMember member, Map<String, User> userMap) {
        FamilyMemberVO vo = new FamilyMemberVO();
        vo.setId(member.getId());
        vo.setFamilyId(member.getFamilyId());
        vo.setUserId(member.getUserId());
        vo.setNickname(member.getNickname());
        vo.setIsOwner(member.getIsOwner());
        vo.setJoinTime(member.getJoinTime());

        User user = userMap.get(member.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserAvatar(user.getAvatar());
        }
        return vo;
    }

    private FamilyJoinApplyVO convertToApplyVO(FamilyJoinApply apply) {
        Map<String, User> userMap = batchQueryUsers(List.of(apply.getUserId()));
        return convertToApplyVO(apply, userMap);
    }

    private FamilyJoinApplyVO convertToApplyVO(FamilyJoinApply apply, Map<String, User> userMap) {
        FamilyJoinApplyVO vo = new FamilyJoinApplyVO();
        vo.setId(apply.getId());
        vo.setFamilyId(apply.getFamilyId());
        vo.setUserId(apply.getUserId());
        vo.setRemark(apply.getRemark());
        vo.setStatus(apply.getStatus());
        vo.setHandleUserId(apply.getHandleUserId());
        vo.setHandleTime(apply.getHandleTime());
        vo.setCreateTime(apply.getCreateTime());

        // 查询家庭名称
        Family family = this.getById(apply.getFamilyId());
        if (family != null) {
            vo.setFamilyName(family.getName());
        }

        // 查询申请人信息
        User user = userMap.get(apply.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserAvatar(user.getAvatar());
        }
        return vo;
    }

    private Map<String, User> batchQueryUsers(List<String> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<User> users = userService.listByIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }
}
