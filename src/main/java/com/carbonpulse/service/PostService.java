package com.carbonpulse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carbonpulse.entity.Post;
import com.carbonpulse.common.PaginatedResult;

import java.util.Map;

public interface PostService extends IService<Post> {

    /**
     * 发布动态
     * @param userId 用户ID
     * @param content 内容
     * @param images 图片JSON字符串
     * @param relatedBehaviorId 关联行为记录ID（可为null）
     * @return 动态ID
     */
    Long createPost(Long userId, String content, String images, Long relatedBehaviorId);

    /**
     * 分页查询动态列表（含作者信息）
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 动态列表
     */
    PaginatedResult<Map<String, Object>> getPostList(int page, int size);

    /**
     * 获取动态详情（含作者信息）
     * @param postId 动态ID
     * @return 动态详情
     */
    Map<String, Object> getPostDetail(Long postId);

    /**
     * 删除动态（会级联删除评论和点赞记录）
     * @param postId 动态ID
     * @param userId 当前用户ID（用于权限校验）
     * @return 是否成功
     */
    boolean deletePost(Long postId, Long userId);



    /**
     * 分页查询动态列表（含作者信息）
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 动态列表
     */
    PaginatedResult<Map<String, Object>> getPostListById(int page, int size,Long userId);
}