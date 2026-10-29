package com.billiard.hall.pay;

import java.math.BigDecimal;

/**
 * 支付抽象接口
 * 充值、消费扣款都通过它下发支付/结算指令。
 * 提供 mock 实现先跑通全流程，后续可扩展微信支付实现。
 */
public interface PayService {

    /**
     * 发起一笔充值
     * @param userId 用户ID
     * @param amount 充值金额
     * @return 是否充值成功（mock 恒成功；真实微信支付由支付回调确认）
     */
    boolean recharge(Long userId, BigDecimal amount);

    /**
     * 发起一笔扣款（消费/结算）
     * @param userId 用户ID
     * @param amount 扣款金额
     * @return 是否扣款成功
     */
    boolean deduct(Long userId, BigDecimal amount);

    /**
     * 退款
     * @param userId 用户ID
     * @param amount 退款金额
     * @return 是否退款成功
     */
    boolean refund(Long userId, BigDecimal amount);
}
