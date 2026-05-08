package com.samoy.chuanbillserver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.dao.UserMapper;
import com.samoy.chuanbillserver.dto.*;
import com.samoy.chuanbillserver.entity.User;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IUserService;
import com.samoy.chuanbillserver.service.IVerificationCodeService;
import com.samoy.chuanbillserver.vo.TokenVO;
import com.samoy.chuanbillserver.vo.UserVO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private IVerificationCodeService verificationCodeService;

    @Resource
    private WxMaService wxMaService;

    @Override
    public TokenVO loginByPassword(LoginByPasswordDTO loginDTO) {
        if (CharSequenceUtil.isBlank(loginDTO.getPhone()) || CharSequenceUtil.isBlank(loginDTO.getPassword())) {
            throw new BusinessException(ResultEnum.PHONE_OR_PASSWORD_MISSING);
        }
        // 查询用户
        User user = getByPhone(loginDTO.getPhone());
        if (user == null) {
            throw new BusinessException(ResultEnum.USER_NOT_FOUND);
        }
        if (ObjectUtil.isEmpty(user.getPassword())) {
            throw new BusinessException(ResultEnum.PASSWORD_NOT_SET, "您未设置密码，请用验证码登录");
        }

        // 验证密码
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultEnum.PASSWORD_ERROR);
        }

        // 生成并返回token
        return genTokenVO(user);
    }

    @Override
    public TokenVO loginByPhone(LoginByPhoneDTO loginDTO) {
        if (CharSequenceUtil.isBlank(loginDTO.getPhone())) {
            throw new BusinessException(ResultEnum.PHONE_MISSING);
        }
        // 验证验证码
        if (!verificationCodeService.verifyCode(loginDTO.getPhone(), loginDTO.getCode())) {
            throw new BusinessException(ResultEnum.TOKEN_INVALID);
        }

        User user = getByPhone(loginDTO.getPhone());
        if (user == null) {
            // 如果用户不存在，创建新用户
            user = new User();
            user.setPhone(loginDTO.getPhone());
            user.setNickname("用户" + PhoneUtil.hideBetween(loginDTO.getPhone()));
            save(user);
        }

        return genTokenVO(user);
    }

    @Override
    public TokenVO loginByWechat(LoginByWechatDTO loginDTO) {
        if (CharSequenceUtil.isBlank(loginDTO.getCode())) {
            throw new BusinessException(ResultEnum.PARAM_VALID_ERROR, "微信登录 code 不能为空");
        }
        try {
            // 通过 code 获取 openid
            WxMaJscode2SessionResult session = wxMaService.jsCode2SessionInfo(loginDTO.getCode());
            String openid = session.getOpenid();
            if (CharSequenceUtil.isBlank(openid)) {
                throw new BusinessException(ResultEnum.LOGIN_ERROR, "获取微信 openid 失败");
            }

            // 通过 openid 查找用户
            User user = getByOpenid(openid);
            if (user == null) {
                // 用户不存在，创建新用户
                user = new User();
                user.setOpenid(openid);
                user.setNickname("微信用户" + RandomUtil.randomString(6));
                save(user);
            }

            return genTokenVO(user);
        } catch (Exception e) {
            throw new BusinessException(ResultEnum.LOGIN_ERROR, "微信登录失败");
        }
    }

    @Override
    public boolean updatePassWordByOld(UpdatePasswordByOldDTO updateDTO) {
        if (CharSequenceUtil.isBlank(updateDTO.getOldPassword())
                || CharSequenceUtil.isBlank(updateDTO.getNewPassword())) {
            throw new BusinessException(ResultEnum.PASSWORD_MISSING);
        }
        User user = getById(updateDTO.getUserId());
        if (user == null) {
            throw new BusinessException(ResultEnum.USER_NOT_FOUND);
        }

        // 验证旧密码
        if (ObjectUtil.isEmpty(user.getPassword())) {
            throw new BusinessException(ResultEnum.PASSWORD_NOT_SET, "您未设置密码，请用验证码设置密码");
        }
        if (!BCrypt.checkpw(updateDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultEnum.PASSWORD_ERROR, "旧密码错误");
        }
        user.setPassword(BCrypt.hashpw(updateDTO.getNewPassword()));
        return updateById(user);
    }

    @Override
    public boolean updatePassWordByCode(UpdatePasswordByCodeDTO updateDTO) {
        if (CharSequenceUtil.isBlank(updateDTO.getPhone()) || CharSequenceUtil.isBlank(updateDTO.getNewPassword())) {
            throw new BusinessException(ResultEnum.PHONE_OR_PASSWORD_MISSING);
        }

        User user = getByPhone(updateDTO.getPhone());
        if (user == null) {
            throw new BusinessException(ResultEnum.USER_NOT_FOUND);
        }

        // 验证验证码
        if (!verificationCodeService.verifyCode(updateDTO.getPhone(), updateDTO.getCode())) {
            throw new BusinessException(ResultEnum.TOKEN_INVALID);
        }

        user.setPassword(BCrypt.hashpw(updateDTO.getNewPassword()));
        return updateById(user);
    }

    @Override
    public boolean updateUserProfile(UserProfileUpdateDTO updateDTO) {
        User user = getById(updateDTO.getUserId());
        if (user == null) {
            throw new BusinessException(ResultEnum.USER_NOT_FOUND);
        }
        // 更新用户信息
        if (!CharSequenceUtil.isBlank(updateDTO.getNickname())) {
            user.setNickname(updateDTO.getNickname());
        }
        if (!CharSequenceUtil.isBlank(updateDTO.getAvatar())) {
            user.setAvatar(updateDTO.getAvatar());
        }
        if (!ObjectUtil.isEmpty(updateDTO.getGender())) {
            user.setGender(updateDTO.getGender());
        }
        return updateById(user);
    }

    @Override
    public UserVO getProfileById(String userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultEnum.USER_NOT_FOUND);
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        // 手机号脱敏
        userVO.setPhone(PhoneUtil.hideBetween(user.getPhone()).toString());
        return userVO;
    }

    @Override
    public boolean hasPassword(String userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultEnum.USER_NOT_FOUND);
        }
        return ObjectUtil.isNotEmpty(user.getPassword());
    }

    private User getByPhone(String phone) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone).eq(User::getStatus, SystemConstants.USER_STATUS_NORMAL);
        return this.getOne(queryWrapper);
    }

    /**
     * 通过 openid 查询用户
     *
     * @param openid 微信 openid
     * @return 用户实体
     */
    private User getByOpenid(String openid) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getOpenid, openid).eq(User::getStatus, SystemConstants.USER_STATUS_NORMAL);
        return this.getOne(queryWrapper);
    }

    @NotNull private TokenVO genTokenVO(User user) {
        // 更新用户最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);

        // 生成token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 设置token信息
        TokenVO tokenVO = new TokenVO();
        tokenVO.setToken(token);
        tokenVO.setUserId(user.getId());
        tokenVO.setNickname(user.getNickname());
        // 将剩余秒数转换为绝对过期时间戳（毫秒）
        long timeoutSeconds = StpUtil.getTokenTimeout();
        long expireAtMillis = timeoutSeconds > 0 ? System.currentTimeMillis() + timeoutSeconds * 1000 : -1;
        tokenVO.setExpireTime(expireAtMillis);
        return tokenVO;
    }

    @Override
    public void logout() {
        String userId = StpUtil.getLoginIdAsString();
        StpUtil.logout(userId);
    }
}
