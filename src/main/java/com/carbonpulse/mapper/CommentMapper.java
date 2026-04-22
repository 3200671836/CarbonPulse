package com.carbonpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carbonpulse.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 根据动态ID查询评论列表（按时间正序）
     */
    List<Comment> selectByPostId(@Param("postId") Long postId);

    List<Map<String, Object>> selectByPostIdWithUser(@Param("postId") Long postId);
}