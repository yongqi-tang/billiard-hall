package com.billiard.hall.pay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 模拟支付实现（开发/演示阶段用）
 * 等接入真实微信支付时，新增实现类并加 @Primary 替换即可，业务代码零改动。
 */
@Slf4j
@Service
public class MockPayService implements PayService {

    @Override
    public boolean recharge(Long userId, BigDecimal amount) {
        log.info("[模拟支付] 用户{} 充值 {} 元", userId, amount);
        return true;
    }

    @Override
    public boolean deduct(Long userId, BigDecimal amount) {
        log.info("[模拟支付] 用户{} 扣款 {} 元", userId, amount);
        return true;
    }

    @Override
    public boolean refund(Long userId, BigDecimal amount) {
        log.info("[模拟支付] 用户{} 退款 {} 元", userId, amount);
        return true;
    }
}
