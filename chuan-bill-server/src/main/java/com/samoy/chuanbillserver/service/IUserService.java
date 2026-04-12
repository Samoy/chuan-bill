package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.dto.*;
import com.samoy.chuanbillserver.entity.User;
import com.samoy.chuanbillserver.vo.TokenVO;
import com.samoy.chuanbillserver.vo.UserVO;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
public interface IUserService extends IService<User> {

    /**
     * 根据手机号和密码登录
     *
     * @param loginDTO 登录信息
     * @return TokenVO
     */
    TokenVO loginByPassword(LoginByPasswordDTO loginDTO);

    /**
     * 根据手机号和验证码登录
     *
     * @param loginDTO 登录信息
     * @return TokenVO
     */
    TokenVO loginByPhone(LoginByPhoneDTO loginDTO);

    /**
     * 微信登录
     *
     * @param loginDTO 登录信息
     * @return TokenVO
     */
    TokenVO loginByWechat(LoginByWechatDTO loginDTO);

    /**
     * 根据旧密码修改密码
     *
     * @param updateDTO 修改信息
     * @return boolean
     */
    boolean updatePassWordByOld(UpdatePasswordByOldDTO updateDTO);

    /**
     * 根据验证码修改密码
     *
     * @param updateDTO 修改信息
     * @return boolean
     */
    boolean updatePassWordByCode(UpdatePasswordByCodeDTO updateDTO);

    /**
     * 修改用户信息
     *
     * @param updateDTO 修改信息
     * @return boolean
     */
    boolean updateUserProfile(UserProfileUpdateDTO updateDTO);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户id
     * @return UserVO
     */
    UserVO getProfileById(String userId);

    /**
     * 判断用户是否存在密码
     *
     * @param userId 用户id
     * @return boolean
     */
    boolean hasPassword(String userId);

    /**
     * 登出
     */
    void logout();
}
