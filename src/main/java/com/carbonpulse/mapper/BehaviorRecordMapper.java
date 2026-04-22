package com.carbonpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carbonpulse.entity.BehaviorRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface BehaviorRecordMapper extends BaseMapper<BehaviorRecord> {

    /**
     * 查询用户最近7天每日减排量
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 列表，元素为 Map，包含 record_date 和 total_carbon
     */
    List<Map<String, Object>> selectWeeklyCarbon(@Param("userId") Long userId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * 查询用户今日记录（返回列表）
     */
    List<BehaviorRecord> selectTodayRecords(@Param("userId") Long userId,
                                            @Param("today") LocalDate today);


    /**
     * 查询历史记录（返回列表）
     */
    List<Map<String, Object>> selectHistory(@Param("userId") Long userId);

    /**
     * 获取用户在指定时间范围内的减排量汇总
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 减排量（克）
     */
    Integer sumCarbonByDateRange(@Param("userId") Long userId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    /**
     * 获取所有用户在指定时间范围内的减排量汇总（用于排行榜）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 列表，包含 userId, totalCarbon
     */
    List<Map<String, Object>> listUserCarbonByDateRange(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);


    int insertBehaviorRecord(BehaviorRecord record);
}
