package com.billiard.hall.controller;

import com.billiard.hall.common.Result;
import com.billiard.hall.entity.BilliardOrder;
import com.billiard.hall.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单接口（小程序端）
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 扫码开台 */
    @PostMapping("/open")
    public Result<BilliardOrder> open(@RequestBody OrderIdReq req) {
        return Result.ok(orderService.open(req.getUserId(), req.getTableId()));
    }

    /** 续时 */
    @PostMapping("/extend")
    public Result<BilliardOrder> extend(@RequestBody OrderIdReq req) {
        return Result.ok(orderService.extend(req.getOrderId(), req.getUserId()));
    }

    /** 结账 */
    @PostMapping("/settle")
    public Result<BilliardOrder> settle(@RequestBody OrderIdReq req) {
        return Result.ok(orderService.settle(req.getOrderId(), req.getUserId()));
    }

    /** 取消订单 */
    @PostMapping("/cancel")
    public Result<BilliardOrder> cancel(@RequestBody OrderIdReq req) {
        return Result.ok(orderService.cancel(req.getOrderId(), req.getUserId()));
    }

    /** 当前进行中的订单（含实时计时/预估金额） */
    @GetMapping("/current")
    public Result<Map<String, Object>> current(@RequestParam Long userId) {
        return Result.ok(orderService.current(userId));
    }

    /** 我的订单历史 */
    @GetMapping("/list")
    public Result<List<BilliardOrder>> myOrders(@RequestParam Long userId) {
        return Result.ok(orderService.myOrders(userId));
    }

    @Data
    public static class OrderIdReq {
        private Long userId;
        private Long tableId;
        private Long orderId;
    }
}
