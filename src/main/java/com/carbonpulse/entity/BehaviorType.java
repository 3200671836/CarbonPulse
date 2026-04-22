package com.carbonpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class BehaviorType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;          // 行为名称
    private String unit;          // 单位（次/公里/分钟）
    private Integer carbonFactor; // 每单位减排系数（克）
    private String icon;          // 图标URL
    private Integer sort;         // 排序
}