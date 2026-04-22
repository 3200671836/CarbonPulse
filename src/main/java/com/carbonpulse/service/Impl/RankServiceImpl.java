package com.carbonpulse.service.Impl;

import com.carbonpulse.mapper.BehaviorRecordMapper;
import com.carbonpulse.mapper.FriendRelationMapper;
import com.carbonpulse.mapper.UserMapper;
import com.carbonpulse.service.RankService;
import com.carbonpulse.utils.CheckinUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RankServiceImpl implements RankService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BehaviorRecordMapper behaviorRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FriendRelationMapper friendRelationMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CheckinUtils checkinUtils;

    // Redis 键
    private static final String GLOBAL_WEEK_RANK = "rank:global:week";
    private static final String GLOBAL_MONTH_RANK = "rank:global:month";
    private static final String FRIEND_RANK_PREFIX = "rank:friends:";

    @Override
    public List<Map<String, Object>> getGlobalWeekRank(int topN) {
        return getRankFromRedis(GLOBAL_WEEK_RANK, topN);
    }

    @Override
    public List<Map<String, Object>> getGlobalMonthRank(int topN) {
        return getRankFromRedis(GLOBAL_MONTH_RANK, topN);
    }

    @Override
    public List<Map<String, Object>> getFriendWeekRank(Long userId, int topN) {
        String key = FRIEND_RANK_PREFIX + userId;
        return getRankFromRedis(key, topN);
    }

    /**
     * 从 Redis SortedSet 获取排行榜数据，并填充用户昵称、头像等信息
     */
    private List<Map<String, Object>> getRankFromRedis(String rankKey, int topN) {
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(rankKey, 0, topN - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long userId = Long.valueOf(tuple.getValue().toString());
            Double carbon = tuple.getScore();
            // 获取用户信息
            Map<String, Object> userInfo = userMapper.selectByIdWithMap(userId);
            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank++);
            item.put("userId", userId);
            item.put("nickname", userInfo.get("nickname"));
            item.put("avatar", userInfo.get("avatar"));
            item.put("carbon", carbon != null ? carbon.longValue() : 0);
            result.add(item);
        }
        return result;
    }

    @Override
    public void updateAllRanks() {
        // 使用分布式锁，防止多实例同时执行
        RLock lock = redissonClient.getLock("rank:update:lock");
        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                try {
                    // 计算周榜范围（本周一到周日）
                    LocalDate today = LocalDate.now();
                    LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                    LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
                    // 计算月榜范围（本月第一天到最后一天）
                    LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
                    LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

                    // 更新全局周榜
                    updateRank(GLOBAL_WEEK_RANK, weekStart, weekEnd);
                    // 更新全局月榜
                    updateRank(GLOBAL_MONTH_RANK, monthStart, monthEnd);
                    // 更新所有用户的好友周榜
                    updateAllFriendRanks();
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败，记录日志
                System.out.println("另一个实例正在更新排行榜，本次跳过");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取排行榜更新锁失败", e);
        }
    }

    /**
     * 更新指定排行榜（先清空，再批量添加）
     */
    private void updateRank(String rankKey, LocalDate start, LocalDate end) {
        List<Map<String, Object>> userCarbonList = behaviorRecordMapper.listUserCarbonByDateRange(start, end);
        redisTemplate.delete(rankKey);
        for (Map<String, Object> map : userCarbonList) {
            // 安全转换 userId
            Object userIdObj = map.get("userId");
            Long userId = ((Number) userIdObj).longValue();   // Number 可以处理 BigInteger、Long 等

            // 安全转换 totalCarbon
            Object carbonObj = map.get("totalCarbon");
            Long totalCarbon = ((Number) carbonObj).longValue(); // 如果数据库中 carbon 可能是小数，可以根据需要改为 doubleValue()

            redisTemplate.opsForZSet().add(rankKey, userId.toString(), totalCarbon);
        }
    }

    /**
     * 更新所有用户的好友周榜（遍历所有用户，为每个用户生成好友榜）
     */
    private void updateAllFriendRanks() {
        List<Long> allUserIds = userMapper.selectAllUserIds();
        for (Long userId : allUserIds) {
            updateFriendRank(userId);
        }
    }

    /**
     * 为单个用户生成好友周榜
     */
    private void updateFriendRank(Long userId) {
        // 获取用户的所有好友ID
        List<Long> friendIds = friendRelationMapper.selectFriendIdsByUserId(userId);
        if (friendIds.isEmpty()) {
            // 空榜单
            redisTemplate.delete(FRIEND_RANK_PREFIX + userId);
            return;
        }
        // 从全局周榜中筛选出好友的分数
        Set<ZSetOperations.TypedTuple<Object>> globalTuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(GLOBAL_WEEK_RANK, 0, -1);
        String rankKey = FRIEND_RANK_PREFIX + userId;
        redisTemplate.delete(rankKey);
        for (ZSetOperations.TypedTuple<Object> tuple : globalTuples) {
            Long friendId = Long.valueOf(tuple.getValue().toString());
            if (friendIds.contains(friendId)) {
                redisTemplate.opsForZSet().add(rankKey, friendId.toString(), tuple.getScore());
            }
        }
        // 可选：将当前用户自己也加入好友榜（如果需要）
        // 可以从全局榜获取当前用户的分数加入
        Double selfScore = redisTemplate.opsForZSet().score(GLOBAL_WEEK_RANK, userId.toString());
        if (selfScore != null) {
            redisTemplate.opsForZSet().add(rankKey, userId.toString(), selfScore);
        }
    }
}