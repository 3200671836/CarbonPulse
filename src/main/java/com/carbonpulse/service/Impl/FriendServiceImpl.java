package com.carbonpulse.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonpulse.common.PaginatedResult;
import com.carbonpulse.entity.FriendRelation;
import com.carbonpulse.entity.User;
import com.carbonpulse.mapper.FriendRelationMapper;
import com.carbonpulse.mapper.PostMapper;
import com.carbonpulse.mapper.UserMapper;
import com.carbonpulse.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FriendServiceImpl extends ServiceImpl<FriendRelationMapper, FriendRelation> implements FriendService {

    @Autowired
    private FriendRelationMapper friendRelationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 好友列表缓存前缀
    private static final String FRIEND_LIST_CACHE_PREFIX = "friend:list:";
    // 缓存过期时间（小时）
    private static final long FRIEND_CACHE_EXPIRE_HOURS = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addFriend(Long userId, Long friendId) {
        // 不能添加自己为好友
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查对方是否存在
        User friend = userMapper.getById(friendId);
        if (friend == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查是否已经是好友
        int count = friendRelationMapper.existsFriendship(userId, friendId);
        if (count > 0) {
            throw new RuntimeException("已经是好友");
        }

        // 双向插入
        FriendRelation relation1 = new FriendRelation();
        relation1.setUserId(userId);
        relation1.setFriendId(friendId);
        relation1.setStatus(1);
        FriendRelation relation2 = new FriendRelation();
        relation2.setUserId(friendId);
        relation2.setFriendId(userId);
        relation2.setStatus(1);

        boolean saved = this.saveBatch(Arrays.asList(relation1, relation2));
        if (saved) {
            // 清除双方的好友列表缓存
            clearFriendListCache(userId);
            clearFriendListCache(friendId);
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFriend(Long userId, Long friendId) {
        // 软删除：将两条记录的状态设为0
        LambdaQueryWrapper<FriendRelation> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(FriendRelation::getUserId, userId)
                .eq(FriendRelation::getFriendId, friendId);
        LambdaQueryWrapper<FriendRelation> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(FriendRelation::getUserId, friendId)
                .eq(FriendRelation::getFriendId, userId);

        FriendRelation update = new FriendRelation();
        update.setStatus(0);

        boolean updated1 = this.update(update, wrapper1);
        boolean updated2 = this.update(update, wrapper2);
        boolean success = updated1 && updated2;

        if (success) {
            // 清除双方的好友列表缓存
            clearFriendListCache(userId);
            clearFriendListCache(friendId);
        }
        return success;
    }

    @Override
    public PaginatedResult<Map<String, Object>> getFriendList(Long userId, int page, int size) {
        // 参数校验
        if (page < 1) page = 1;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;

        String cacheKey = FRIEND_LIST_CACHE_PREFIX + userId + ":" + page + ":" + size;
        // 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                PaginatedResult<Map<String, Object>> result = (PaginatedResult<Map<String, Object>>) cached;
                return result;
            } catch (Exception e) {
                // 反序列化失败，继续查库
            }
        }

        // 计算分页偏移量
        int offset = (page - 1) * size;
        // 查询好友ID列表（已分页，但需要先获取全部ID再分页？这里用数据库分页）
        // 由于好友数量不会特别大，可以直接用分页查询好友关系表获取ID，再批量查用户信息
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)
                .eq(FriendRelation::getStatus, 1)
                .last("LIMIT " + offset + "," + size);
        List<FriendRelation> relations = this.list(wrapper);
        if (relations.isEmpty()) {
            PaginatedResult<Map<String, Object>> emptyResult = new PaginatedResult<>(Collections.emptyList(), 0, page, size);
            cacheResult(cacheKey, emptyResult);
            return emptyResult;
        }

        // 获取好友ID列表
        List<Long> friendIds = relations.stream()
                .map(FriendRelation::getFriendId)
                .toList();

        // 批量查询用户信息（只取必要字段）
        List<User> friends = userMapper.selectBatchIds(friendIds);
        List<Map<String, Object>> friendList = new ArrayList<>();
        for (User friend : friends) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", friend.getId());
            map.put("nickname", friend.getNickname());
            map.put("avatar", friend.getAvatar());
            // 可添加碳减排量等其他信息
            map.put("totalCarbon", friend.getTotalCarbon());
            friendList.add(map);
        }

        // 获取总好友数（状态正常）
        long total = this.count(new LambdaQueryWrapper<FriendRelation>()
                .eq(FriendRelation::getUserId, userId)
                .eq(FriendRelation::getStatus, 1));

        PaginatedResult<Map<String, Object>> result = new PaginatedResult<>(friendList, total, page, size);
        cacheResult(cacheKey, result);
        return result;
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)
                .eq(FriendRelation::getFriendId, friendId)
                .eq(FriendRelation::getStatus, 1);
        return this.count(wrapper) > 0;
    }

    @Override
    public PaginatedResult<Map<String, Object>> getFriendPosts(Long userId, int page, int size) {
        if (page < 1) page = 1;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;

        // 获取所有好友ID
        List<Long> friendIds = friendRelationMapper.selectFriendIdsByUserId(userId);
        if (friendIds.isEmpty()) {
            return new PaginatedResult<>(Collections.emptyList(), 0, page, size);
        }

        // 分页查询好友发布的动态
        int offset = (page - 1) * size;
        // 使用自定义 SQL 查询（需要 PostMapper 支持）
        List<Map<String, Object>> posts = postMapper.selectPostsByUserIds(friendIds, offset, size);
        // 查询总数
        long total = postMapper.countPostsByUserIds(friendIds);

        return new PaginatedResult<>(posts, total, page, size);
    }

    /**
     * 清除指定用户的好友列表缓存
     */
    private void clearFriendListCache(Long userId) {
        String pattern = FRIEND_LIST_CACHE_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 缓存分页结果
     */
    private void cacheResult(String key, PaginatedResult<Map<String, Object>> result) {
        redisTemplate.opsForValue().set(key, result, FRIEND_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }
}