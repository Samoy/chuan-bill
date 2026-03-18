package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.MessageMapper;
import com.samoy.chuanbillserver.entity.Message;
import com.samoy.chuanbillserver.service.IMessageService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 消息表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {}
