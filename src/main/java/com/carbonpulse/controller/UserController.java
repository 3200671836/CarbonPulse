package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.entity.User;
import com.carbonpulse.listener.WebSocketEventListener;
import com.carbonpulse.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.carbonpulse.service.UserService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/api/user")
@RestController
@Tag(name = "用户管理", description = "用户注册、登录、个人信息相关接口")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WebSocketEventListener webSocketEventListener;

    @Operation(summary = "用户注册", description = "新用户注册，需提供用户名、密码和昵称")
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        try {
            userService.register(user);
            return Result.success("注册成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT Token和用户信息")
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginMap) {
        String username = loginMap.get("username");
        String password = loginMap.get("password");
        try {
            String token = userService.login(username, password);
            User user = userService.getUserByUsername(username);
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取当前用户信息", description = "需JWT认证，返回当前登录用户的详细信息")
    @GetMapping("/info")
    public Result getUserInfo() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null) {
            return Result.error("未登录");
        }
        Long userId = (Long) principal;
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "修改用户信息", description = "需JWT认证，可修改昵称、头像等字段")
    @PutMapping("/update")
    public Result updateProfile(@RequestBody User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return Result.error("未登录");
        }
        Long userId = (Long) authentication.getPrincipal();

        user.setId(userId);
        boolean success = userService.updateUserInfo(user);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    @Operation(summary = "获取指定用户信息", description = "根据userId获取用户详情，含在线状态")
    @GetMapping("/profile")
    public Result getUserById(
            @Parameter(description = "目标用户ID", required = true)
            @RequestParam Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        String status = webSocketEventListener.getUserStatus(userId);
        user.setStatus(status);
        return Result.success(user);
    }
}
