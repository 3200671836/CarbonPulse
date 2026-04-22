package com.carbonpulse.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonpulse.common.PaginatedResult;
import com.carbonpulse.entity.PrivateConversation;
import com.carbonpulse.entity.PrivateMessage;
import com.carbonpulse.entity.User;
import com.carbonpulse.mapper.PrivateConversationMapper;
import com.carbonpulse.mapper.PrivateMessageMapper;
import com.carbonpulse.mapper.UserMapper;
import com.carbonpulse.service.PrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class PrivateMessageServiceImpl extends ServiceImpl<PrivateMessageMapper, PrivateMessage> implements PrivateMessageService {

    @Autowired
    private PrivateConversationMapper conversationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String UNREAD_CACHE_PREFIX = "unread:total:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivateMessage sendMessage(Long fromUserId, Long toUserId, String content, Integer type) {
        // 1. 获取或创建会话
        PrivateConversation conversation = conversationMapper.selectByUsers(fromUserId, toUserId);
        if (conversation == null) {
            conversation = new PrivateConversation();
            Long userId1 = Math.min(fromUserId, toUserId);
            Long userId2 = Math.max(fromUserId, toUserId);
            conversation.setUserId1(userId1);
            conversation.setUserId2(userId2);
            conversation.setUnreadCountUser1(0);
            conversation.setUnreadCountUser2(0);
            conversationMapper.insert(conversation);
        }

        // 2. 保存消息
        PrivateMessage message = new PrivateMessage();
        message.setConversationId(conversation.getId());
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setType(type != null ? type : 1);
        message.setIsRead(false);
        message.setCreateTime(LocalDateTime.now());
        this.save(message);

        // 3. 更新会话的最后消息和未读数
        conversation.setLastMessage(content);
        conversation.setLastMessageTime(LocalDateTime.now());
        // 根据接收者增加未读
        if (conversation.getUserId1().equals(toUserId)) {
            conversation.setUnreadCountUser1(conversation.getUnreadCountUser1() + 1);
        } else {
            conversation.setUnreadCountUser2(conversation.getUnreadCountUser2() + 1);
        }
        conversationMapper.updateById(conversation);

        // 4. 清除接收者的总未读缓存
        String cacheKey = UNREAD_CACHE_PREFIX + toUserId;
        redisTemplate.delete(cacheKey);

        // 5. 通过 WebSocket 实时推送消息给接收方和发送方
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", message.getId());
        payload.put("conversationId", conversation.getId());
        payload.put("fromUserId", fromUserId);
        payload.put("toUserId", toUserId);
        payload.put("content", content);
        payload.put("type", message.getType());
        payload.put("createTime", message.getCreateTime());

        // 发送给接收方
        messagingTemplate.convertAndSendToUser(toUserId.toString(), "/queue/messages", payload);

        // 发送给发送方（确认消息已保存，前端可更新本地临时消息）
        messagingTemplate.convertAndSendToUser(fromUserId.toString(), "/queue/messages", payload);

        // 6. 聚合通知：不直接发送系统通知，由前端轮询未读数，或发送一个"有新消息"的轻量通知
        // 这里发送一个"未读变化"事件，前端收到后重新拉取未读数
        messagingTemplate.convertAndSendToUser(toUserId.toString(), "/queue/unread", "refresh");

        return message;
    }

    @Override
    public List<Map<String, Object>> getConversationList(Long userId) {
        // 查询用户所有会话
        LambdaQueryWrapper<PrivateConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(PrivateConversation::getUserId1, userId).or().eq(PrivateConversation::getUserId2, userId));
        wrapper.orderByDesc(PrivateConversation::getLastMessageTime);
        List<PrivateConversation> conversations = conversationMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (PrivateConversation conv : conversations) {
            Long otherUserId = conv.getUserId1().equals(userId) ? conv.getUserId2() : conv.getUserId1();
            User otherUser = userMapper.selectById(otherUserId);
            int unread = conv.getUserId1().equals(userId) ? conv.getUnreadCountUser1() : conv.getUnreadCountUser2();

            // 从 Redis 获取对方在线状态
            String onlineStatus = "offline";
            try {
                String onlineKey = "user:online:" + otherUserId;
                Boolean isOnline = redisTemplate.hasKey(onlineKey);
                onlineStatus = Boolean.TRUE.equals(isOnline) ? "online" : "offline";
            } catch (Exception e) {
                // Redis 不可用时默认离线，不影响会话列表加载
                System.err.println("Redis查询在线状态失败: " + e.getMessage());
            }

            Map<String, Object> item = new HashMap<>();
            item.put("conversationId", conv.getId());
            item.put("otherUserId", otherUserId);
            item.put("otherNickname", otherUser != null ? otherUser.getNickname() : "用户");
            item.put("otherAvatar", otherUser != null ? otherUser.getAvatar() : null);
            item.put("lastMessage", conv.getLastMessage());
            item.put("lastMessageTime", conv.getLastMessageTime());
            item.put("unreadCount", unread);
            item.put("onlineStatus", onlineStatus);
            result.add(item);
        }
        return result;
    }

    @Override
    public PaginatedResult<PrivateMessage> getChatHistory(Long userId, Long otherUserId, int page, int size) {
        if (page < 1) page = 1;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        int offset = (page - 1) * size;

        PrivateConversation conv = conversationMapper.selectByUsers(userId, otherUserId);
        if (conv == null) {
            return new PaginatedResult<>(Collections.emptyList(), 0, page, size);
        }
        List<PrivateMessage> messages = baseMapper.selectByConversationPage(conv.getId(), offset, size);
        long total = this.count(new LambdaQueryWrapper<PrivateMessage>().eq(PrivateMessage::getConversationId, conv.getId()));
        return new PaginatedResult<>(messages, total, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markConversationRead(Long userId, Long otherUserId) {
        PrivateConversation conv = conversationMapper.selectByUsers(userId, otherUserId);
        if (conv == null) return;
        // 更新消息表已读状态
        baseMapper.markAsReadByConversation(conv.getId(), userId);
        // 更新会话表未读计数
        if (conv.getUserId1().equals(userId)) {
            conv.setUnreadCountUser1(0);
        } else {
            conv.setUnreadCountUser2(0);
        }
        conversationMapper.updateById(conv);
        // 清除总未读缓存
        redisTemplate.delete(UNREAD_CACHE_PREFIX + userId);
        // 可选：通过 WebSocket 通知发送方该会话已读（用于显示已读回执）
        Map<String, Object> ack = new HashMap<>();
        ack.put("conversationId", conv.getId());
        ack.put("readByUserId", userId);
        messagingTemplate.convertAndSendToUser(otherUserId.toString(), "/queue/read-receipt", ack);
    }

    @Override
    public int getTotalUnreadCount(Long userId) {
        String cacheKey = UNREAD_CACHE_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (int) cached;
            }
        } catch (Exception e) {
            System.err.println("Redis读取未读缓存失败: " + e.getMessage());
        }
        // 查询所有会话中该用户的未读数之和
        LambdaQueryWrapper<PrivateConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(PrivateConversation::getUserId1, userId).or().eq(PrivateConversation::getUserId2, userId));
        List<PrivateConversation> list = conversationMapper.selectList(wrapper);
        int total = list.stream().mapToInt(conv -> {
            if (conv.getUserId1().equals(userId)) return conv.getUnreadCountUser1();
            else return conv.getUnreadCountUser2();
        }).sum();
        try {
            redisTemplate.opsForValue().set(cacheKey, total, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("Redis写入未读缓存失败: " + e.getMessage());
        }
        return total;
    }
}