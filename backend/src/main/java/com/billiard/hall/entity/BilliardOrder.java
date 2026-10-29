package com.billiard.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("billiard_order")
public class BilliardOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    private Long userId;

    private Long tableId;

    /** 开台时间 */
    private LocalDateTime startTime;

    /** 结账时间 */
    private LocalDateTime endTime;

    /** 使用时长（分钟） */
    private Integer durationMin;

    /** 消费金额 */
    private BigDecimal amount;

    /** 状态 0进行中 1已结账 2已取消 3欠费 */
    private Integer status;

    /** 支付状态 0未支付 1已支付 */
    private Integer payStatus;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
