package com.carbonpulse.service;

import java.util.List;
import java.util.Map;

public interface RankService {

    /**
     * 获取全局周榜
     * @param topN 返回前N名
     * @return 列表，包含 userId, nickname, carbon
     */
    List<Map<String, Object>> getGlobalWeekRank(int topN);

    /**
     * 获取全局月榜
     * @param topN 返回前N名
     * @return 列表，包含 userId, nickname, carbon
     */
    List<Map<String, Object>> getGlobalMonthRank(int topN);

    /**
     * 获取好友周榜（当前用户好友）
     * @param userId 当前用户ID
     * @param topN 返回前N名
     * @return 列表，包含 userId, nickname, carbon
     */
    List<Map<String, Object>> getFriendWeekRank(Long userId, int topN);

    /**
     * 每日定时任务：更新周榜、月榜、好友榜
     */
    void updateAllRanks();
}