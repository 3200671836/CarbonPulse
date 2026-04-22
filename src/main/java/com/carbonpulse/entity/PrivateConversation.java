package com.carbonpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateConversation {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id_1")
    private Long userId1;

    @TableField("user_id_2")
    private Long userId2;

    @TableField("last_message")
    private String lastMessage;

    @TableField("last_message_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageTime;

    @TableField("unread_count_user1")
    private Integer unreadCountUser1;

    @TableField("unread_count_user2")
    private Integer unreadCountUser2;

    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}