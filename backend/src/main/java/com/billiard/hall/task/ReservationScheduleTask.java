package com.billiard.hall.task;

import com.billiard.hall.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：预约超时/过期自动释放
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduleTask {

    private final ReservationService reservationService;

    /** 每 60 秒扫描一次 */
    @Scheduled(fixedDelay = 60000)
    public void checkReservationTimeout() {
        try {
            reservationService.releaseTimeoutReservations();
        } catch (Exception e) {
            log.error("预约超时扫描异常", e);
        }
    }
}
