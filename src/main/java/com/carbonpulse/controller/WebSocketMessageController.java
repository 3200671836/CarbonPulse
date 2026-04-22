package com.carbonpulse.controller;

import com.carbonpulse.entity.PrivateMessage;
import com.carbonpulse.service.PrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketMessageController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PrivateMessageService privateMessageService;

    /**
     * 处理私信发送
     * 前端发送到：/app/chat.send
     * 消息体：{ "toUserId": 123, "content": "hello", "type": 1 }
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long fromUserId = Long.valueOf(principal.getName());
        Long toUserId = Long.valueOf(payload.get("toUserId").toString());
        String content = (String) payload.get("content");
        Integer type = payload.get("type") != null ? (Integer) payload.get("type") : 1;
        privateMessageService.sendMessage(fromUserId, toUserId, content, type);
    }

    /**
     * 处理已读回执（用户进入会话时调用）
     * 前端发送到：/app/chat.read
     * 消息体：{ "otherUserId": 123 }
     */
    @MessageMapping("/chat.read")
    public void markRead(@Payload Map<String, Object> payload, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        Long otherUserId = Long.valueOf(payload.get("otherUserId").toString());
        privateMessageService.markConversationRead(userId, otherUserId);
    }

    /**
     * 处理输入状态提示（正在输入）
     * 前端发送到：/app/chat.typing
     * 消息体：{ "toUserId": 123, "typing": true }
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, Object> payload, Principal principal) {
        Long fromUserId = Long.valueOf(principal.getName());
        Long toUserId = Long.valueOf(payload.get("toUserId").toString());
        Boolean typing = (Boolean) payload.get("typing");
        // 转发给接收方
        messagingTemplate.convertAndSendToUser(toUserId.toString(), "/queue/typing",
                Map.of("fromUserId", fromUserId, "typing", typing));
    }
}