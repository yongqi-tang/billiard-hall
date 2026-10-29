package com.billiard.hall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置（读 application.yml 的 billiard.wx 段）
 */
@Data
@Component
@ConfigurationProperties(prefix = "billiard.wx")
public class WxProperties {

    private String appid;

    private String secret;
}
