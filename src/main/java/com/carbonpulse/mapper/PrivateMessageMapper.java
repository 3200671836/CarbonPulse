package com.carbonpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carbonpulse.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    /**
     * 分页查询会话消息，按时间正序排列（最早的在前）
     */
    @Select("SELECT * FROM private_message WHERE conversation_id = #{conversationId} ORDER BY create_time ASC LIMIT #{offset}, #{size}")
    List<PrivateMessage> selectByConversationPage(@Param("conversationId") Long conversationId,
                                                  @Param("offset") int offset,
                                                  @Param("size") int size);

    @Update("UPDATE private_message SET is_read = 1 WHERE conversation_id = #{conversationId} AND to_user_id = #{userId} AND is_read = 0")
    int markAsReadByConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}