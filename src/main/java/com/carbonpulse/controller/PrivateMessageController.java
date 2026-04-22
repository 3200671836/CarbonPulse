package com.carbonpulse.controller;

import com.carbonpulse.common.PaginatedResult;
import com.carbonpulse.common.Result;
import com.carbonpulse.entity.PrivateMessage;
import com.carbonpulse.service.PrivateMessageService;
import com.carbonpulse.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
@Tag(name = "私信消息", description = "私信会话管理、聊天记录、未读统计、消息发送")
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

    @Operation(summary = "获取总未读消息数", description = "返回当前用户所有会话的未读消息总数，用于红点提示")
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

    @Operation(summary = "获取会话列表", description = "返回当前用户的所有私信会话，含对方用户信息、最后一条消息、未读数、在线状态")
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

    @Operation(summary = "获取聊天记录（分页）", description = "获取与指定用户的聊天记录，按时间升序排列，支持分页")
    @GetMapping("/history/{otherUserId}")
    public Result getChatHistory(
            @Parameter(description = "对方用户ID") @PathVariable Long otherUserId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            PaginatedResult<PrivateMessage> history = privateMessageService.getChatHistory(userId, otherUserId, page, size);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "标记会话已读", description = "将指定会话中对方发来的未读消息全部标记为已读")
    @PostMapping("/read/{otherUserId}")
    public Result markConversationRead(
            @Parameter(description = "对方用户ID") @PathVariable Long otherUserId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            privateMessageService.markConversationRead(userId, otherUserId);
            return Result.success("已读成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "发送私信（HTTP）", description = "HTTP方式发送私信，作为WebSocket断连时的降级方案")
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
