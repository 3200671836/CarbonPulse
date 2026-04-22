package com.carbonpulse.task;

import com.carbonpulse.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RankUpdateTask {

    @Autowired
    private RankService rankService;

    // 每天凌晨 1 点执行
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateRanks() {
        System.out.println("开始更新排行榜...");
        rankService.updateAllRanks();
        System.out.println("排行榜更新完成");
    }
}