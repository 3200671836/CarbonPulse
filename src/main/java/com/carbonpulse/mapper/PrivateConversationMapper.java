package com.carbonpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carbonpulse.entity.PrivateConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PrivateConversationMapper extends BaseMapper<PrivateConversation> {

    @Select("SELECT * FROM private_conversation WHERE (user_id_1 = #{a} AND user_id_2 = #{b}) OR (user_id_1 = #{b} AND user_id_2 = #{a})")
    PrivateConversation selectByUsers(@Param("a") Long userId1, @Param("b") Long userId2);

    @Update("UPDATE private_conversation SET unread_count_user1 = 0 WHERE id = #{conversationId} AND user_id_1 = #{userId}")
    int clearUnreadForUser1(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Update("UPDATE private_conversation SET unread_count_user2 = 0 WHERE id = #{conversationId} AND user_id_2 = #{userId}")
    int clearUnreadForUser2(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}