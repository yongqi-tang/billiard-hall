package com.billiard.hall.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.billiard.hall.common.BusinessException;
import com.billiard.hall.config.WxProperties;
import com.billiard.hall.entity.User;
import com.billiard.hall.mapper.UserMapper;
import com.billiard.hall.pay.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户业务：微信登录、余额充值
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final WxProperties wxProperties;
    private final PayService payService;

    /**
     * 微信登录：
     * 1) 用 code 调微信 code2session 换 openid
     * 2) 首次登录自动注册，返回用户
     *
     * 开发模式：code 以 dev_ 开头（或空）时，直接生成测试 openid，跳过真实微信调用
     */
    public User login(String code) {
        String openid;
        if (code == null || code.startsWith("dev_") || "your-appid".equals(wxProperties.getAppid())) {
            // 模拟登录（本地联调 / 未配置真实 AppID）
            String suffix = (code == null || code.isBlank()) ? IdUtil.fastSimpleUUID().substring(0, 8) : code.substring(4);
            openid = "dev_openid_" + suffix;
            log.info("[模拟登录] code={} -> openid={}", code, openid);
        } else {
            openid = code2session(code);
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("球友" + openid.substring(openid.length() - 4));
            user.setBalance(BigDecimal.ZERO);
            user.setMemberLevel(0);
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);
            log.info("新用户注册 openid={}", openid);
        }
        return user;
    }

    /**
     * 调微信 code2session 接口换 openid
     */
    private String code2session(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + wxProperties.getAppid()
                + "&secret=" + wxProperties.getSecret()
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        String resp = HttpUtil.get(url, 5000);
        JSONObject json = JSONUtil.parseObj(resp);
        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            throw new BusinessException("微信登录失败: " + json.getStr("errmsg"));
        }
        return json.getStr("openid");
    }

    /**
     * 充值（开发阶段为模拟支付，真实版换成微信支付回调）
     */
    public User recharge(Long userId, BigDecimal amount) {
        User user = getById(userId);
        // 走支付抽象：mock 直接成功；真实微信支付由回调确认后再入账
        if (!payService.recharge(userId, amount)) {
            throw new BusinessException("支付失败，请稍后再试");
        }
        user.setBalance(user.getBalance().add(amount));
        userMapper.updateById(user);
        return user;
    }

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
