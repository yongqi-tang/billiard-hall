package com.billiard.hall.controller;

import com.billiard.hall.common.BusinessException;
import com.billiard.hall.common.Result;
import com.billiard.hall.entity.BilliardOrder;
import com.billiard.hall.entity.BilliardTable;
import com.billiard.hall.entity.User;
import com.billiard.hall.service.AdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口（Vue 管理端用）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** 管理员登录 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginReq req) {
        if (!adminService.login(req.getUsername(), req.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("token", "admin-token");
        data.put("username", req.getUsername());
        return Result.ok(data);
    }

    /** 球台新增/修改 */
    @PostMapping("/table/save")
    public Result<BilliardTable> saveTable(@RequestBody BilliardTable table) {
        return Result.ok(adminService.saveTable(table));
    }

    /** 删除球台 */
    @DeleteMapping("/table/{id}")
    public Result<Void> deleteTable(@PathVariable Long id) {
        adminService.deleteTable(id);
        return Result.ok();
    }

    /** 订单列表 */
    @GetMapping("/order/list")
    public Result<List<BilliardOrder>> orderList() {
        return Result.ok(adminService.allOrders());
    }

    /** 用户列表 */
    @GetMapping("/user/list")
    public Result<List<User>> userList() {
        return Result.ok(adminService.allUsers());
    }

    /** 营收统计 */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.ok(adminService.statistics());
    }

    /** 后台给用户充值 */
    @PostMapping("/user/recharge")
    public Result<User> adminRecharge(@RequestBody RechargeReq req) {
        return Result.ok(adminService.adminRecharge(req.getUserId(), req.getAmount()));
    }

    @Data
    public static class LoginReq {
        private String username;
        private String password;
    }

    @Data
    public static class RechargeReq {
        private Long userId;
        private BigDecimal amount;
    }
}
