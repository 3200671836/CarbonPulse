package com.carbonpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Setter
@Getter
public class BehaviorRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long behaviorTypeId;
    private BigDecimal value;          // 数量
    private Integer carbonReduction;   // 本次减排量（克）
    private LocalDate recordDate;      // 记录日期
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}