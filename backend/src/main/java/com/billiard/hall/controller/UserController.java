package com.billiard.hall.controller;

import cn.hutool.core.util.IdUtil;
import com.billiard.hall.common.Result;
import com.billiard.hall.entity.User;
import com.billiard.hall.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 微信登录
     * 入参: { code: "wx.login拿到的code 或 dev_xxx(模拟)" }
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginReq req) {
        User user = userService.login(req.getCode());
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        // 简化鉴权：直接返回 userId 当 token（后续可升级 JWT）
        data.put("token", IdUtil.simpleUUID() + ":" + user.getId());
        return Result.ok(data);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public Result<User> info(@RequestParam Long userId) {
        return Result.ok(userService.getById(userId));
    }

    /**
     * 模拟支付充值
     * 入参: { userId, amount }
     */
    @PostMapping("/recharge")
    public Result<User> recharge(@RequestBody RechargeReq req) {
        return Result.ok(userService.recharge(req.getUserId(), req.getAmount()));
    }

    @Data
    public static class LoginReq {
        private String code;
    }

    @Data
    public static class RechargeReq {
        private Long userId;
        private BigDecimal amount;
    }
}
