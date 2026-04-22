package com.carbonpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carbonpulse.entity.LikeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {

    /**
     * 检查用户是否已点赞某动态
     */
    int existsLike(@Param("postId") Long postId, @Param("userId") Long userId);
}