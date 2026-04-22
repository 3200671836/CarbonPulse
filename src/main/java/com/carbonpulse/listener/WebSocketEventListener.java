package com.carbonpulse.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class WebSocketEventListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_STATUS_PREFIX = "user:online:";

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (userId != null) {
            // 标记用户在线，设置 Redis 缓存
            String key = ONLINE_STATUS_PREFIX + userId;
            redisTemplate.opsForValue().set(key, "online", 30, TimeUnit.MINUTES);

            // 广播用户上线状态
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("userId", Long.valueOf(userId));
            statusUpdate.put("status", "online");
            messagingTemplate.convertAndSend("/topic/user-status", statusUpdate);

            System.out.println("用户上线: userId=" + userId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (userId != null) {
            // 标记用户离线，删除 Redis 缓存
            String key = ONLINE_STATUS_PREFIX + userId;
            redisTemplate.delete(key);

            // 广播用户离线状态
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("userId", Long.valueOf(userId));
            statusUpdate.put("status", "offline");
            messagingTemplate.convertAndSend("/topic/user-status", statusUpdate);

            System.out.println("用户离线: userId=" + userId);
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        String key = ONLINE_STATUS_PREFIX + userId;
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取用户状态
     */
    public String getUserStatus(Long userId) {
        return isUserOnline(userId) ? "online" : "offline";
    }
}