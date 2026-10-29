package com.billiard.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水实体
 */
@Data
@TableName("recharge_record")
public class RechargeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 变动金额（充值正/消费负） */
    private BigDecimal amount;

    /** 类型 1充值 2消费 3退款 */
    private Integer type;

    private Long orderId;

    private LocalDateTime createTime;
}
