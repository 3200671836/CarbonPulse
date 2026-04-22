package com.carbonpulse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carbonpulse.entity.Comment;

public interface CommentService extends IService<Comment> {

    /**
     * 添加评论
     * @param postId 动态ID
     * @param userId 评论人ID
     * @param content 评论内容
     * @return 评论ID
     */
    Long addComment(Long postId, Long userId, String content);

    /**
     * 删除评论（只有作者或动态作者可删除）
     * @param commentId 评论ID
     * @param userId 当前用户ID
     * @return 是否成功
     */
    boolean deleteComment(Long commentId, Long userId);
}