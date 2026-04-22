package com.carbonpulse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carbonpulse.entity.LikeRecord;

public interface LikeRecordService extends IService<LikeRecord> {

    /**
     * 点赞或取消点赞
     * @param postId 动态ID
     * @param userId 用户ID
     * @return true 表示点赞成功，false 表示取消点赞成功
     */
    boolean likeOrUnlike(Long postId, Long userId);
}