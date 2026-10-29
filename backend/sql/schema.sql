-- =====================================================
-- 自主台球厅系统 数据库初始化脚本
-- 使用方法：先 CREATE DATABASE billiard 再执行本脚本
-- =====================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `openid`      VARCHAR(64)  NOT NULL COMMENT '微信openid（唯一标识）',
    `nickname`    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `balance`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    `member_level` TINYINT     NOT NULL DEFAULT 0 COMMENT '会员等级 0普通 1白银 2黄金',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 球台表
CREATE TABLE IF NOT EXISTS `billiard_table` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '球台ID',
    `name`        VARCHAR(64)  NOT NULL COMMENT '球台名称，如 A区1号台',
    `location`    VARCHAR(128) DEFAULT NULL COMMENT '位置描述',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0空闲 1使用中 2维修 3预约',
    `price_start` DECIMAL(10,2) NOT NULL DEFAULT 30.00 COMMENT '起步价',
    `price_hour`  DECIMAL(10,2) NOT NULL DEFAULT 30.00 COMMENT '每小时价格',
    `device_id`   BIGINT       DEFAULT NULL COMMENT '绑定的灯光设备ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球台表';

-- 订单表（核心）
CREATE TABLE IF NOT EXISTS `billiard_order` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`    VARCHAR(32)  NOT NULL COMMENT '订单号',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `table_id`    BIGINT       NOT NULL COMMENT '球台ID',
    `start_time`  DATETIME     DEFAULT NULL COMMENT '开台时间',
    `end_time`    DATETIME     DEFAULT NULL COMMENT '结账时间',
    `duration_min` INT         NOT NULL DEFAULT 0 COMMENT '使用时长（分钟）',
    `amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '消费金额',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0进行中 1已结账 2已取消 3欠费',
    `pay_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '支付状态 0未支付 1已支付',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 充值/流水表
CREATE TABLE IF NOT EXISTS `recharge_record` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `amount`      DECIMAL(10,2) NOT NULL COMMENT '变动金额（充值正/消费负）',
    `type`        TINYINT      NOT NULL COMMENT '类型 1充值 2消费 3退款',
    `order_id`    BIGINT       DEFAULT NULL COMMENT '关联订单ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水表';

-- 灯光设备表（球台灯）
CREATE TABLE IF NOT EXISTS `device` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    `device_id`    VARCHAR(64)  NOT NULL COMMENT '设备平台标识',
    `provider`     VARCHAR(32)  NOT NULL DEFAULT 'mock' COMMENT '设备平台：mock/涂鸦/机智云/ESP8266',
    `name`         VARCHAR(64)  DEFAULT NULL COMMENT '设备名称',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '开关状态 0关 1开',
    `bind_table_id` BIGINT      DEFAULT NULL COMMENT '绑定的球台ID',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灯光设备表';

-- 初始化测试数据（球台 + 设备，本地联调用）
-- 注意：billiard_table.device_id 指向 device 表主键 id，用于开台/结账联动灯光
INSERT INTO `billiard_table` (`name`, `location`, `status`, `price_start`, `price_hour`, `device_id`) VALUES
('A区1号台', 'A区靠窗', 0, 30.00, 30.00, 1),
('A区2号台', 'A区靠窗', 0, 30.00, 30.00, 2),
('B区1号台', 'B区中间', 0, 25.00, 25.00, NULL),
('B区2号台', 'B区中间', 0, 25.00, 25.00, NULL);

INSERT INTO `device` (`device_id`, `provider`, `name`, `status`, `bind_table_id`) VALUES
('mock-001', 'mock', '模拟灯-1号台', 0, 1),
('mock-002', 'mock', '模拟灯-2号台', 0, 2);
