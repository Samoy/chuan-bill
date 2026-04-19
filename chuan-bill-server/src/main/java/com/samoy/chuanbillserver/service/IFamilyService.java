package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.dto.CreateFamilyDTO;
import com.samoy.chuanbillserver.dto.HandleJoinApplyDTO;
import com.samoy.chuanbillserver.dto.JoinFamilyDTO;
import com.samoy.chuanbillserver.dto.RemoveMemberDTO;
import com.samoy.chuanbillserver.dto.TransferOwnerDTO;
import com.samoy.chuanbillserver.dto.UpdateFamilyDTO;
import com.samoy.chuanbillserver.entity.Family;
import com.samoy.chuanbillserver.vo.FamilyJoinApplyVO;
import com.samoy.chuanbillserver.vo.FamilyMemberVO;
import com.samoy.chuanbillserver.vo.FamilyVO;
import java.util.List;

/**
 * <p>
 * 家庭表 服务类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
public interface IFamilyService extends IService<Family> {

    /**
     * 创建家庭
     */
    FamilyVO createFamily(String userId, CreateFamilyDTO dto);

    /**
     * 更新家庭信息
     */
    FamilyVO updateFamily(String userId, UpdateFamilyDTO dto);

    /**
     * 获取家庭详情
     */
    FamilyVO getFamilyDetail(String userId, String familyId);

    /**
     * 删除家庭
     */
    boolean deleteFamily(String userId, String familyId);

    /**
     * 获取用户所在家庭列表
     */
    List<FamilyVO> getMyFamilies(String userId);

    /**
     * 通过邀请码加入家庭（提交申请）
     */
    FamilyJoinApplyVO joinFamily(String userId, JoinFamilyDTO dto);

    /**
     * 退出家庭
     */
    boolean leaveFamily(String userId, String familyId);

    /**
     * 移除家庭成员
     */
    boolean removeMember(String userId, RemoveMemberDTO dto);

    /**
     * 转让户主
     */
    boolean transferOwner(String userId, TransferOwnerDTO dto);

    /**
     * 获取家庭成员列表
     */
    List<FamilyMemberVO> getMembers(String userId, String familyId);

    /**
     * 获取待处理的加入申请列表
     */
    List<FamilyJoinApplyVO> getPendingApplies(String userId, String familyId);

    /**
     * 处理加入申请
     */
    boolean handleJoinApply(String userId, HandleJoinApplyDTO dto);

    /**
     * 刷新邀请码
     */
    String refreshInviteCode(String userId, String familyId);

    /**
     * 检查用户是否是家庭成员
     */
    boolean isMember(String userId, String familyId);

    /**
     * 检查用户是否是户主
     */
    boolean isOwner(String userId, String familyId);
}
