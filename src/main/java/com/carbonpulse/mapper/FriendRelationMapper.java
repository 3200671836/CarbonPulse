package com.carbonpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carbonpulse.entity.FriendRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendRelationMapper extends BaseMapper<FriendRelation> {

    /**
     * 获取用户的所有好友ID列表（状态正常）
     */
    @Select("SELECT friend_id FROM friend_relation WHERE user_id = #{userId} AND status = 1")
    List<Long> selectFriendIdsByUserId(@Param("userId") Long userId);

    /**
     * 检查是否已经是好友（双向）
     */
    @Select("SELECT COUNT(*) FROM friend_relation WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 1")
    int existsFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);
}