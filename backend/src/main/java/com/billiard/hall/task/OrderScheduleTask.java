package com.billiard.hall.task;

import com.billiard.hall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：欠费超时强制结账
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduleTask {

    private final OrderService orderService;

    /** 每 60 秒扫描一次 */
    @Scheduled(fixedDelay = 60000)
    public void checkDebt() {
        try {
            orderService.checkTimeout();
        } catch (Exception e) {
            log.error("欠费扫描异常", e);
        }
    }
}
