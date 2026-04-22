package com.carbonpulse.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;

@Component
public class CheckinUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Bitmap key 格式：checkin:user:{userId}:year:month
    // 例如 2025年3月：checkin:user:123:2025:03
    public String getCheckinKey(Long userId, int year, int month) {
        return String.format("checkin:user:%d:%d:%02d", userId, year, month);
    }

    /**
     * 用户打卡
     * @param userId 用户ID
     * @param date 打卡日期
     */
    public void checkin(Long userId, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        String key = getCheckinKey(userId, year, month);
        // 设置对应 bit 为 1（偏移量从0开始，所以 day-1）
        redisTemplate.opsForValue().setBit(key, day - 1, true);
    }

    /**
     * 获取用户连续打卡天数（截止到今天）
     * @param userId 用户ID
     * @param today 今天日期
     * @return 连续天数
     */
    public int getConsecutiveDays(Long userId, LocalDate today) {
        int consecutive = 0;
        LocalDate current = today;
        // 向前遍历直到遇到未打卡的日期
        while (true) {
            boolean checked = isCheckedIn(userId, current);
            if (checked) {
                consecutive++;
                current = current.minusDays(1);
            } else {
                break;
            }
        }
        return consecutive;
    }

    /**
     * 判断用户在指定日期是否打卡
     * @param userId 用户ID
     * @param date 日期
     * @return true/false
     */
    public boolean isCheckedIn(Long userId, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        String key = getCheckinKey(userId, year, month);
        Boolean bit = redisTemplate.opsForValue().getBit(key, day - 1);
        return Boolean.TRUE.equals(bit);
    }
}