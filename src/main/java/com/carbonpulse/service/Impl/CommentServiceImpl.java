package com.carbonpulse.service.Impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonpulse.entity.Comment;
import com.carbonpulse.entity.Post;
import com.carbonpulse.mapper.CommentMapper;
import com.carbonpulse.mapper.PostMapper;
import com.carbonpulse.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private PostMapper postMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(Long postId, Long userId, String content) {
        // 检查动态是否存在
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("动态不存在");
        }

        // 创建评论
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        this.save(comment);

        // 更新动态的评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteComment(Long commentId, Long userId) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        // 权限校验：评论作者 或 动态作者 可以删除
        Post post = postMapper.selectById(comment.getPostId());
        if (!comment.getUserId().equals(userId) && !post.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此评论");
        }

        // 删除评论
        boolean removed = this.removeById(commentId);
        if (removed) {
            // 更新动态的评论数
            post.setCommentCount(post.getCommentCount() - 1);
            postMapper.updateById(post);
        }
        return removed;
    }
}