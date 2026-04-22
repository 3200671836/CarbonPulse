package com.carbonpulse.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carbonpulse.entity.User;

public interface UserService   {

    /**
     * 用户注册
     * @param user 用户信息（用户名、密码、昵称）
     * @return 是否注册成功
     */
    boolean register(User user);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return JWT token
     */
    String login(String username, String password);

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户对象
     */
    User getUserByUsername(String username);

    /**
     * 更新用户信息（昵称、头像等）
     * @param user 包含更新字段的用户对象
     * @return 是否更新成功
     */
    boolean updateUserInfo(User user);

    User getById(Long userId);
}