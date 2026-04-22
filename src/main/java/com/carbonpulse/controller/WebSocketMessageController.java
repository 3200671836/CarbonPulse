package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.service.PrivateMessageService;
import com.carbonpulse.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@Tag(name = "WebSocket消息", description = "STOMP协议实时通信：发送消息、已读回执、输入状态")
public class WebSocketMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "发送消息（STOMP）", description = "通过WebSocket发送私信，目标地址: /app/chat.send")
    @MessageMapping("/chat.send")
    public void sendMessage(Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;
        Long userId = Long.valueOf(principal.getName());
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String content = (String) payload.get("content");
        Integer type = payload.get("type") != null ? Integer.valueOf(payload.get("type").toString()) : 1;
        privateMessageService.sendMessage(userId, receiverId, content, type);
    }

    @Operation(summary = "标记已读（STOMP）", description = "通过WebSocket标记消息已读，目标地址: /app/chat.read")
    @MessageMapping("/chat.read")
    public void markRead(Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;
        Long userId = Long.valueOf(principal.getName());
        Long otherUserId = Long.valueOf(payload.get("otherUserId").toString());
        privateMessageService.markConversationRead(userId, otherUserId);
    }

    @Operation(summary = "输入状态（STOMP）", description = "通知对方自己正在输入，目标地址: /app/chat.typing")
    @MessageMapping("/chat.typing")
    public void typing(Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        // 输入状态通知已在前端通过 /user/queue/typing 订阅处理
    }
}
