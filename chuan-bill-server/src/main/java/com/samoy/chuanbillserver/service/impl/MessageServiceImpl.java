package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.MessageMapper;
import com.samoy.chuanbillserver.dto.MessageListDTO;
import com.samoy.chuanbillserver.entity.Message;
import com.samoy.chuanbillserver.expection.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IMessageService;
import com.samoy.chuanbillserver.vo.MessageVO;
import com.samoy.chuanbillserver.vo.UnreadCountVO;
import java.util.List;
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
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    @Override
    public IPage<MessageVO> getMessageList(String userId, MessageListDTO dto) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId);

        if (ObjectUtil.isNotEmpty(dto.getType())) {
            wrapper.eq(Message::getType, dto.getType());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(Message::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(Message::getCreateTime);

        IPage<Message> page = this.page(new Page<>(dto.getPage(), dto.getSize()), wrapper);
        return page.convert(this::convertToMessageVO);
    }

    @Override
    public boolean markAsRead(String userId, String messageId) {
        Message message = this.getById(messageId);
        if (message == null) {
            throw new BusinessException(ResultEnum.MESSAGE_NOT_FOUND);
        }
        if (!message.getUserId().equals(userId)) {
            throw new BusinessException(ResultEnum.MESSAGE_NOT_FOUND);
        }
        message.setStatus(1);
        return this.updateById(message);
    }

    @Override
    public boolean markAllAsRead(String userId) {
        List<Message> unreadList = this.list(
                new LambdaQueryWrapper<Message>().eq(Message::getUserId, userId).eq(Message::getStatus, 0));
        if (unreadList.isEmpty()) {
            return true;
        }
        unreadList.forEach(m -> m.setStatus(1));
        return this.updateBatchById(unreadList);
    }

    @Override
    public UnreadCountVO getUnreadCount(String userId) {
        long total = this.count(
                new LambdaQueryWrapper<Message>().eq(Message::getUserId, userId).eq(Message::getStatus, 0));
        long familyCount = this.count(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getStatus, 0)
                .eq(Message::getType, "family"));

        UnreadCountVO vo = new UnreadCountVO();
        vo.setTotal((int) total);
        vo.setFamilyCount((int) familyCount);
        return vo;
    }

    @Override
    public void sendMessage(
            String userId, String title, String content, String type, String relatedId, String relatedType) {
        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setStatus(0);
        message.setRelatedId(relatedId);
        message.setRelatedType(relatedType);
        this.save(message);
    }

    private MessageVO convertToMessageVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setType(message.getType());
        vo.setStatus(message.getStatus());
        vo.setRelatedId(message.getRelatedId());
        vo.setRelatedType(message.getRelatedType());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
