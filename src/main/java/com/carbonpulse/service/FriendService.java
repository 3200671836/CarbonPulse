package com.carbonpulse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carbonpulse.entity.FriendRelation;
import com.carbonpulse.common.PaginatedResult;

import java.util.List;
import java.util.Map;

public interface FriendService extends IService<FriendRelation> {

    /**
     * 添加好友
     * @param userId 当前用户ID
     * @param friendId 要添加的好友ID
     * @return 是否成功
     */
    boolean addFriend(Long userId, Long friendId);

    /**
     * 删除好友（软删除）
     * @param userId 当前用户ID
     * @param friendId 要删除的好友ID
     * @return 是否成功
     */
    boolean deleteFriend(Long userId, Long friendId);

    /**
     * 获取好友列表（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 好友列表，包含用户基本信息
     */
    PaginatedResult<Map<String, Object>> getFriendList(Long userId, int page, int size);

    /**
     * 检查是否好友关系
     */
    boolean isFriend(Long userId, Long friendId);

    /**
     * 获取好友动态（分页）
     * @param userId 当前用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 动态列表（仅包含好友发布的动态）
     */
    PaginatedResult<Map<String, Object>> getFriendPosts(Long userId, int page, int size);
}