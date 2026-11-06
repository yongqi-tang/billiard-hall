package com.billiard.hall.controller;

import com.billiard.hall.common.Result;
import com.billiard.hall.entity.Reservation;
import com.billiard.hall.service.ReservationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 预约接口（小程序端）
 */
@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** 创建预约 */
    @PostMapping("/create")
    public Result<Reservation> create(@RequestBody CreateReq req) {
        return Result.ok(reservationService.create(
                req.getUserId(), req.getTableId(), req.getReserveDate(),
                req.getStartTime(), req.getEndTime(), req.getRemark()));
    }

    /** 我的预约列表 */
    @GetMapping("/list")
    public Result<List<Reservation>> myList(@RequestParam Long userId) {
        return Result.ok(reservationService.myReservations(userId));
    }

    /** 取消预约 */
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestBody CancelReq req) {
        reservationService.cancel(req.getReservationId(), req.getUserId());
        return Result.ok();
    }

    /** 到店开台（预约核销） */
    @PostMapping("/checkin")
    public Result<Reservation> checkin(@RequestBody CheckinReq req) {
        return Result.ok(reservationService.checkin(
                req.getReservationId(), req.getUserId(), req.getTableId()));
    }

    /** 管理端：全部预约 */
    @GetMapping("/admin/list")
    public Result<List<Reservation>> adminList() {
        return Result.ok(reservationService.allReservations());
    }

    /** 管理端：取消预约 */
    @PostMapping("/admin/cancel")
    public Result<Void> adminCancel(@RequestBody AdminCancelReq req) {
        reservationService.adminCancel(req.getReservationId());
        return Result.ok();
    }

    @Data
    public static class CreateReq {
        private Long userId;
        private Long tableId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate reserveDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String remark;
    }

    @Data
    public static class CancelReq {
        private Long reservationId;
        private Long userId;
    }

    @Data
    public static class CheckinReq {
        private Long reservationId;
        private Long userId;
        private Long tableId;
    }

    @Data
    public static class AdminCancelReq {
        private Long reservationId;
    }
}
