package com.carbonpulse.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonpulse.entity.LikeRecord;
import com.carbonpulse.entity.Post;
import com.carbonpulse.mapper.LikeRecordMapper;
import com.carbonpulse.mapper.PostMapper;
import com.carbonpulse.service.LikeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord> implements LikeRecordService {

    @Autowired
    private PostMapper postMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeOrUnlike(Long postId, Long userId) {
        // 检查动态是否存在
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("动态不存在");
        }

        // 检查是否已点赞
        int exists = baseMapper.existsLike(postId, userId);
        if (exists > 0) {
            // 取消点赞
            this.remove(new LambdaQueryWrapper<LikeRecord>()
                    .eq(LikeRecord::getPostId, postId)
                    .eq(LikeRecord::getUserId, userId));
            // 更新动态点赞数
            post.setLikeCount(post.getLikeCount() - 1);
            postMapper.updateById(post);
            return false;
        } else {
            // 添加点赞
            LikeRecord record = new LikeRecord();
            record.setPostId(postId);
            record.setUserId(userId);
            this.save(record);
            // 更新动态点赞数
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            return true;
        }
    }
}