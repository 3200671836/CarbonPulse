package com.carbonpulse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carbonpulse.entity.PrivateConversation;
import com.carbonpulse.entity.PrivateMessage;
import com.carbonpulse.common.PaginatedResult;

import java.util.List;
import java.util.Map;

public interface PrivateMessageService extends IService<PrivateMessage> {

    /**
     * 发送私信（同时更新会话）
     */
    PrivateMessage sendMessage(Long fromUserId, Long toUserId, String content, Integer type);

    /**
     * 获取用户会话列表（包含对方基本信息、最后消息、未读数）
     */
    List<Map<String, Object>> getConversationList(Long userId);

    /**
     * 获取与某人的聊天记录（分页）
     */
    PaginatedResult<PrivateMessage> getChatHistory(Long userId, Long otherUserId, int page, int size);

    /**
     * 标记某个会话的所有消息为已读
     */
    void markConversationRead(Long userId, Long otherUserId);

    /**
     * 获取用户总未读消息数（所有会话未读之和）
     */
    int getTotalUnreadCount(Long userId);
}