package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.service.FriendService;
import com.carbonpulse.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
public class FriendController {

    @Autowired
    private FriendService friendService;

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
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 添加好友
     * POST /api/friend/add
     * 请求体：{ "friendId": 123 }
     */
    @PostMapping("/add")
    public Result addFriend(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Long friendId = Long.valueOf(params.get("friendId").toString());
            boolean success = friendService.addFriend(userId, friendId);
            return success ? Result.success("添加好友成功") : Result.error("添加好友失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除好友
     * DELETE /api/friend/delete/{friendId}
     */
    @DeleteMapping("/delete/{friendId}")
    public Result deleteFriend(@PathVariable Long friendId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean success = friendService.deleteFriend(userId, friendId);
            return success ? Result.success("删除好友成功") : Result.error("删除好友失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 好友列表（分页）
     * GET /api/friend/list?page=1&size=10
     */
    @GetMapping("/list")
    public Result getFriendList(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(friendService.getFriendList(userId, page, size));
        } catch (Exception e) {
            return Result.error("获取好友列表失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否好友关系
     * GET /api/friend/check/{friendId}
     */
    @GetMapping("/check/{friendId}")
    public Result checkFriend(@PathVariable Long friendId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean isFriend = friendService.isFriend(userId, friendId);
            return Result.success(isFriend);
        } catch (Exception e) {
            return Result.error("检查好友关系失败: " + e.getMessage());
        }
    }

    /**
     * 好友动态（分页）
     * GET /api/friend/posts?page=1&size=10
     */
    @GetMapping("/posts")
    public Result getFriendPosts(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(friendService.getFriendPosts(userId, page, size));
        } catch (Exception e) {
            return Result.error("获取好友动态失败: " + e.getMessage());
        }
    }
}