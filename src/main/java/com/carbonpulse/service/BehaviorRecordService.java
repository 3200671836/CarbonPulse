package com.carbonpulse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carbonpulse.entity.BehaviorRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BehaviorRecordService extends IService<BehaviorRecord> {

    /**
     * 记录行为
     * @param userId 用户ID
     * @param behaviorTypeId 行为类型ID
     * @param value 数量
     * @return 本次减排量
     */
    int recordBehavior(Long userId, Long behaviorTypeId, BigDecimal value);

    /**
     * 获取用户今日行为记录列表
     */
    List<BehaviorRecord> getTodayRecords(Long userId);

    /**
     * 获取用户个人看板数据（累计、连续、今日减排量）
     */
    Map<String, Object> getDashboard(Long userId);

    /**
     * 获取用户近7天减排趋势
     */
    List<Map<String, Object>> getWeeklyTrend(Long userId);


    /**
     * 获取用户今日行为记录列表
     */
    List<Map<String, Object>>  getHistory(Long userId);
}