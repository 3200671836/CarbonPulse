package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.entity.BehaviorType;
import com.carbonpulse.service.BehaviorRecordService;
import com.carbonpulse.service.BehaviorTypeService;
import com.carbonpulse.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/behavior")
public class BehaviorRecordController {

    @Autowired
    private BehaviorRecordService behaviorRecordService;

    @Autowired
    private JwtUtil jwtUtil;


    @Autowired
    private BehaviorTypeService behaviorTypeService;

    @GetMapping("/types")
    public List<BehaviorType> getAllBehaviorTypes() {
        return behaviorTypeService.findAll();
    }

    @GetMapping("/type/{id}")
    public BehaviorType getBehaviorTypeById(@PathVariable Long id) {
        return behaviorTypeService.findById(id);
    }

    @DeleteMapping("/type/{id}")
    public void deleteBehaviorTypeById(@PathVariable Long id) {
        behaviorTypeService.deleteById(id);
    }


    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("未登录");
        }
        // 如果 token 以 "Bearer " 开头，需去除
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 记录行为
     * POST /api/behavior/record
     * 请求体：{ "behaviorTypeId": 1, "value": 3.5 }
     */
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

    /**
     * 获取今日记录列表
     */
    @GetMapping("/today")
    public Result getTodayRecords(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(behaviorRecordService.getTodayRecords(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取个人看板数据
     */
    @GetMapping("/dashboard")
    public Result getDashboard(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(behaviorRecordService.getDashboard(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取近7天减排趋势
     */
    @GetMapping("/weekly")
    public Result getWeeklyTrend(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.success(behaviorRecordService.getWeeklyTrend(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


    /**
     * 获取历史数据
     */
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