package com.samoy.chuanbillserver.service.impl;

import com.samoy.chuanbillserver.entity.User;
import com.samoy.chuanbillserver.dao.UserMapper;
import com.samoy.chuanbillserver.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

}
