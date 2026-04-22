package com.carbonpulse.service.Impl;

import com.carbonpulse.mapper.UserMapper;
import com.carbonpulse.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.carbonpulse.entity.User;
import com.carbonpulse.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean register(User user) {
        User exist = userMapper.selectByUsername(user.getUsername());
        if (exist != null) {
            // 用户已存在，抛出异常让 Controller 捕获并返回错误
            throw new RuntimeException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setTotalCarbon(0);
        user.setConsecutiveDays(0);
        user.setLastRecordDate(null);
        return userMapper.register(user);
    }

    @Override
    public String login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        // 生成 JWT token（通常包含 userId 和 username）
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public User getUserByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user != null) {
            return user;
        }
        return null;
    }

    @Override
    public boolean updateUserInfo(User user) {
        // 只允许更新 nickname、avatar
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setNickname(user.getNickname());
        updateUser.setAvatar(user.getAvatar());
        return true;
    }

    @Override
    public User getById(Long userId) {
        User user = userMapper.getById(userId);
        if (user != null) {
            return user;
        }
        return null;
    }
}