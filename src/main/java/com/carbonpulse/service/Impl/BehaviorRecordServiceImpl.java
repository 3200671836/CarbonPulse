package com.carbonpulse.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonpulse.entity.BehaviorRecord;
import com.carbonpulse.entity.BehaviorType;
import com.carbonpulse.entity.User;
import com.carbonpulse.mapper.BehaviorRecordMapper;
import com.carbonpulse.mapper.BehaviorTypeMapper;
import com.carbonpulse.mapper.UserMapper;
import com.carbonpulse.service.BehaviorRecordService;
import com.carbonpulse.utils.CheckinUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BehaviorRecordServiceImpl extends ServiceImpl<BehaviorRecordMapper, BehaviorRecord> implements BehaviorRecordService {

    @Autowired
    private BehaviorRecordMapper behaviorRecordMapper;

    @Autowired
    private BehaviorTypeMapper behaviorTypeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CheckinUtils checkinUtils; // 注入打卡工具类

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recordBehavior(Long userId, Long behaviorTypeId, BigDecimal value) {
        // 1. 查询行为类型，获取减排系数
        BehaviorType behaviorType = behaviorTypeMapper.selectById(behaviorTypeId);
        if (behaviorType == null) {
            throw new RuntimeException("行为类型不存在");
        }

        // 2. 计算减排量（克）
        int carbonReduction = behaviorType.getCarbonFactor() * value.intValue();

        // 3. 创建行为记录
        BehaviorRecord record = new BehaviorRecord();
        record.setUserId(userId);
        record.setBehaviorTypeId(behaviorTypeId);
        record.setValue(value);
        record.setCarbonReduction(carbonReduction);
        record.setRecordDate(LocalDate.now());
        int rows = behaviorRecordMapper.insertBehaviorRecord(record);
        if (rows != 1) {
            throw new RuntimeException("记录保存失败");
        }

        // 4. 更新用户累计减排量
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        int newTotal = user.getTotalCarbon() + carbonReduction;
        user.setTotalCarbon(newTotal);

        // 5. 使用 Redis Bitmap 处理打卡
        LocalDate today = LocalDate.now();
        // 打卡
        checkinUtils.checkin(userId, today);
        // 计算连续打卡天数（从今天开始往前数）
        int consecutiveDays = checkinUtils.getConsecutiveDays(userId, today);
        user.setConsecutiveDays(consecutiveDays);
        user.setLastRecordDate(today);

        // 6. 持久化用户信息
        userMapper.updateById(user);

        return carbonReduction;
    }

    //查询用户今日记录（返回列表）
    @Override
    public List<BehaviorRecord> getTodayRecords(Long userId) {
        return baseMapper.selectTodayRecords(userId, LocalDate.now());
    }


    //获取个人看板数据
    @Override
    public Map<String, Object> getDashboard(Long userId) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 计算今日减排量
        List<BehaviorRecord> todayRecords = getTodayRecords(userId);
        int todayCarbon = todayRecords.stream().mapToInt(BehaviorRecord::getCarbonReduction).sum();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalCarbon", user.getTotalCarbon());
        dashboard.put("consecutiveDays", user.getConsecutiveDays());
        dashboard.put("todayCarbon", todayCarbon);
        return dashboard;
    }


    //获取7天内的减排记录（每日数据）
    @Override
    public List<Map<String, Object>> getWeeklyTrend(Long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        return baseMapper.selectWeeklyCarbon(userId, startDate, endDate);
    }


    //获取历史记录
    @Override
    public List<Map<String, Object>>  getHistory(Long userId) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        //查询历史记录
        return baseMapper.selectHistory(userId);
    }
}