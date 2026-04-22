package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.entity.BehaviorType;
import com.carbonpulse.service.BehaviorRecordService;
import com.carbonpulse.service.BehaviorTypeService;
import com.carbonpulse.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/behavior")
@Tag(name = "低碳行为", description = "行为类型管理、行为记录、看板数据、减排趋势")
public class BehaviorRecordController {

    @Autowired
    private BehaviorRecordService behaviorRecordService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BehaviorTypeService behaviorTypeService;

    @Operation(summary = "获取所有行为类型", description = "返回系统中全部低碳行为类型列表")
    @GetMapping("/types")
    public List<BehaviorType> getAllBehaviorTypes() {
        return behaviorTypeService.findAll();
    }

    @Operation(summary = "获取行为类型详情", description = "根据ID获取单个行为类型信息")
    @GetMapping("/type/{id}")
    public BehaviorType getBehaviorTypeById(
            @Parameter(description = "行为类型ID") @PathVariable Long id) {
        return behaviorTypeService.findById(id);
    }

    @Operation(summary = "删除行为类型", description = "根据ID删除行为类型")
    @DeleteMapping("/type/{id}")
    public void deleteBehaviorTypeById(
            @Parameter(description = "行为类型ID") @PathVariable Long id) {
        behaviorTypeService.deleteById(id);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("未登录");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    @Operation(summary = "记录低碳行为", description = "提交一次低碳行为记录，系统自动计算减排量")
    @PostMapping("/record")
    public Result recordBehavior(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Long behaviorTypeId = Long.valueOf(params.get("behaviorTypeId").toString());
            BigDecimal value = new BigDecimal(params.get("value").toString());
            int carbon = behaviorRecordService.recordBehavior(userId, behaviorTypeId, value);
            return Result.success("记录成功，本次减排 " + carbon + " 克");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取今日行为记录", description = "返回当前用户今日的所有低碳行为记录")
    @GetMapping("/today")
    public Result getTodayRecords(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(behaviorRecordService.getTodayRecords(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取个人看板数据", description = "返回当前用户的减排统计概览（总减排、连续天数、排名等）")
    @GetMapping("/dashboard")
    public Result getDashboard(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(behaviorRecordService.getDashboard(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取近7天减排趋势", description = "返回当前用户最近7天的每日减排量折线图数据")
    @GetMapping("/weekly")
    public Result getWeeklyTrend(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(behaviorRecordService.getWeeklyTrend(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取历史行为记录", description = "返回当前用户的历史行为记录列表")
    @GetMapping("/history")
    public Result getHistory(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(behaviorRecordService.getHistory(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
