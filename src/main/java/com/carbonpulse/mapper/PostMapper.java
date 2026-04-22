package com.carbonpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carbonpulse.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 分页查询动态列表，同时关联用户信息
     * @param offset 起始位置
     * @param size 每页数量
     * @return 动态列表，每条动态包含作者昵称、头像等信息
     */
    List<Map<String, Object>> selectPostListWithUser(@Param("offset") int offset, @Param("size") int size);


    @Select("SELECT COUNT(*) FROM post")
    long selectPostCount();


    /**
     * 根据多个用户ID分页查询动态（包含作者信息）
     */
    List<Map<String, Object>> selectPostsByUserIds(@Param("userIds") List<Long> userIds,
                                                   @Param("offset") int offset,
                                                   @Param("size") int size);

    /**
     * 统计多个用户发布的动态总数
     */
    long countPostsByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 根据单个用户ID分页查询动态
     */
    List<Map<String, Object>> selectPostsByUserId(@Param("userId") Long userId,
                                                   @Param("offset") int offset,
                                                   @Param("size") int size);


    /**
     * 统计单个用户发布的动态总数
     */
    long selectPostCountByUserId(@Param("userId") Long userId);
}