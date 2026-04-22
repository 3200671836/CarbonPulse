package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.service.FriendService;
import com.carbonpulse.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
@Tag(name = "好友管理", description = "添加好友、删除好友、好友列表、好友动态")
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

    @Operation(summary = "添加好友", description = "向指定用户发送好友请求，双向建立好友关系")
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

    @Operation(summary = "删除好友", description = "解除与指定用户的好友关系")
    @DeleteMapping("/delete/{friendId}")
    public Result deleteFriend(
            @Parameter(description = "好友用户ID") @PathVariable Long friendId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean success = friendService.deleteFriend(userId, friendId);
            return success ? Result.success("删除好友成功") : Result.error("删除好友失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "好友列表（分页）", description = "获取当前用户的好友列表，含在线状态")
    @GetMapping("/list")
    public Result getFriendList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(friendService.getFriendList(userId, page, size));
        } catch (Exception e) {
            return Result.error("获取好友列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "检查好友关系", description = "判断当前用户与指定用户是否为好友")
    @GetMapping("/check/{friendId}")
    public Result checkFriend(
            @Parameter(description = "目标用户ID") @PathVariable Long friendId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean isFriend = friendService.isFriend(userId, friendId);
            return Result.success(isFriend);
        } catch (Exception e) {
            return Result.error("检查好友关系失败: " + e.getMessage());
        }
    }

    @Operation(summary = "好友动态（分页）", description = "获取当前用户好友发布的动态列表")
    @GetMapping("/posts")
    public Result getFriendPosts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(friendService.getFriendPosts(userId, page, size));
        } catch (Exception e) {
            return Result.error("获取好友动态失败: " + e.getMessage());
        }
    }
}
