package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.FamilyMemberMapper;
import com.samoy.chuanbillserver.entity.FamilyMember;
import com.samoy.chuanbillserver.service.IFamilyMemberService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 家庭成员表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Service
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember>
        implements IFamilyMemberService {}
