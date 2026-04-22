package com.carbonpulse.controller;

import com.carbonpulse.common.PaginatedResult;
import com.carbonpulse.common.Result;
import com.carbonpulse.entity.PrivateMessage;
import com.carbonpulse.service.PrivateMessageService;
import com.carbonpulse.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class PrivateMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("未登录");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("token无效");
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 获取总未读消息数（红点）
     * GET /api/message/unread/total
     */
    @GetMapping("/unread/total")
    public Result getTotalUnread(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            int total = privateMessageService.getTotalUnreadCount(userId);
            return Result.success(total);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取会话列表
     * GET /api/message/conversations
     */
    @GetMapping("/conversations")
    public Result getConversationList(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            List<Map<String, Object>> list = privateMessageService.getConversationList(userId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取与某人的聊天记录（分页）
     * GET /api/message/history/{otherUserId}?page=1&size=20
     */
    @GetMapping("/history/{otherUserId}")
    public Result getChatHistory(@PathVariable Long otherUserId,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            PaginatedResult<PrivateMessage> history = privateMessageService.getChatHistory(userId, otherUserId, page, size);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记某个会话已读（批量已读）
     * POST /api/message/read/{otherUserId}
     */
    @PostMapping("/read/{otherUserId}")
    public Result markConversationRead(@PathVariable Long otherUserId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            privateMessageService.markConversationRead(userId, otherUserId);
            return Result.success("已读成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * HTTP发送私信（WebSocket降级方案）
     * POST /api/message/send
     * 请求体：{ "receiverId": 123, "content": "hello", "type": 1 }
     */
    @PostMapping("/send")
    public Result sendMessage(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            String content = (String) payload.get("content");
            Integer type = payload.get("type") != null ? Integer.valueOf(payload.get("type").toString()) : 1;
            PrivateMessage message = privateMessageService.sendMessage(userId, receiverId, content, type);
            return Result.success(message);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
