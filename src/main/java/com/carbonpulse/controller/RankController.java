package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.service.RankService;
import com.carbonpulse.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rank")
public class RankController {

    @Autowired
    private RankService rankService;

    @Autowired
    private JwtUtil jwtUtil;


    @PostMapping("/updateRanks")
    public String updateRanks() {
        rankService.updateAllRanks();
        return "排行榜更新已触发";
    }

    /**
     * 获取当前用户ID
     */
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

    /**
     * 全局周榜
     * @param topN 返回前几名，默认10
     */
    @GetMapping("/global/week")
    public Result getGlobalWeekRank(@RequestParam(defaultValue = "10") int topN) {
        try {
            List<Map<String, Object>> rank = rankService.getGlobalWeekRank(topN);
            return Result.success(rank);
        } catch (Exception e) {
            return Result.error("获取周榜失败: " + e.getMessage());
        }
    }

    /**
     * 全局月榜
     */
    @GetMapping("/global/month")
    public Result getGlobalMonthRank(@RequestParam(defaultValue = "10") int topN) {
        try {
            List<Map<String, Object>> rank = rankService.getGlobalMonthRank(topN);
            return Result.success(rank);
        } catch (Exception e) {
            return Result.error("获取月榜失败: " + e.getMessage());
        }
    }

    /**
     * 好友周榜
     */
    @GetMapping("/friends/week")
    public Result getFriendWeekRank(@RequestParam(defaultValue = "10") int topN,
                                    HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            List<Map<String, Object>> rank = rankService.getFriendWeekRank(userId, topN);
            return Result.success(rank);
        } catch (Exception e) {
            return Result.error("获取好友周榜失败: " + e.getMessage());
        }
    }
}