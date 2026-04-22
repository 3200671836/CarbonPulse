package com.carbonpulse.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carbonpulse.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper  extends BaseMapper<User> {
    // 如果有自定义 SQL 可在此添加方法，对应 UserMapper.xml
    User selectByUsername(String username);

    User getById(Long userId);

    boolean register(User user);

    Map<String, Object> selectByIdWithMap(@Param("id") Long id);


    /**
     * 获取所有用户的ID列表
     * @return 用户ID列表
     */
    List<Long> selectAllUserIds();
}