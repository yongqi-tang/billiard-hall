package com.billiard.hall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 计费规则配置（读 application.yml 的 billiard 段）
 */
@Data
@Component
@ConfigurationProperties(prefix = "billiard")
public class BilliardProperties {

    /** 起步价含的分钟数 */
    private Integer startPriceMinutes = 30;

    /** 起步价 */
    private BigDecimal startPrice = new BigDecimal("30.00");

    /** 超过起步后每分钟单价 */
    private BigDecimal pricePerMinute = new BigDecimal("1.00");

    /** 余额不足欠费后 N 分钟强制结账灭灯 */
    private Integer debtTimeoutMinutes = 30;
}
