package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.entity.User;
import com.carbonpulse.listener.WebSocketEventListener;
import com.carbonpulse.utils.JwtUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.carbonpulse.service.UserService;

import java.util.HashMap;
import java.util.Map;

@Slf4j // 记录日志
@RequestMapping("/api/user")
@RestController
@CrossOrigin // 支持跨域
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WebSocketEventListener webSocketEventListener;

    /**
     * 用户注册
     * @param user 用户信息（username, password, nickname）
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        try {
            userService.register(user);
            return Result.success("注册成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     * @param loginMap { username, password }
     * @return token 及用户基本信息
     */
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

    /**
     * 获取当前登录用户信息（需认证）
     * @return 用户详情
     */
    @GetMapping("/info")
    public Result getUserInfo() {
        // 从 SecurityContext 获取当前认证的用户 ID（在过滤器中存入的 principal）
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

    /**
     * 修改用户信息（昵称、头像）
     * @param user 包含修改字段的用户对象
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result updateProfile(@RequestBody User user) {
        // 从 SecurityContext 获取当前认证的用户 ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return Result.error("未登录");
        }
        Long userId = (Long) authentication.getPrincipal();

        user.setId(userId);
        boolean success = userService.updateUserInfo(user);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 获取用户信息
     * @return 用户详情（含在线状态）
     */
    @GetMapping("/profile")
    public Result getUserById(@RequestParam Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        // 设置用户在线状态
        String status = webSocketEventListener.getUserStatus(userId);
        user.setStatus(status);
        return Result.success(user);
    }
}