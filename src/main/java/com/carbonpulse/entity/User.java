package com.carbonpulse.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Setter
@Getter
public class User {
    //设计用户表 user：id, username, password（加密存储）, nickname, avatar, total_carbon（累计减排量，单位克）,
    // consecutive_days（连续打卡天数）, last_record_date（最后记录日期）, create_time。
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private Integer totalCarbon;       // 累计减排量（克）
    private Integer consecutiveDays;   // 连续打卡天数
    private LocalDate lastRecordDate;  // 最后记录日期
    @TableField(exist = false)
    private String status;             // 用户状态：online/offline（非数据库字段，从Redis获取）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}