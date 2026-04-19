package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.dto.MessageListDTO;
import com.samoy.chuanbillserver.entity.Message;
import com.samoy.chuanbillserver.vo.MessageVO;
import com.samoy.chuanbillserver.vo.UnreadCountVO;

/**
 * <p>
 * 消息表 服务类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
public interface IMessageService extends IService<Message> {

    /**
     * 分页获取消息列表
     */
    IPage<MessageVO> getMessageList(String userId, MessageListDTO dto);

    /**
     * 标记消息已读
     */
    boolean markAsRead(String userId, String messageId);

    /**
     * 标记所有消息已读
     */
    boolean markAllAsRead(String userId);

    /**
     * 获取未读消息数量
     */
    UnreadCountVO getUnreadCount(String userId);

    /**
     * 发送消息（内部调用）
     */
    void sendMessage(String userId, String title, String content, String type, String relatedId, String relatedType);
}
