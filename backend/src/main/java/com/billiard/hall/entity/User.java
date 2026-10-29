package com.billiard.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信openid（唯一标识） */
    private String openid;

    private String nickname;

    private String avatar;

    private String phone;

    /** 账户余额 */
    private BigDecimal balance;

    /** 会员等级 0普通 1白银 2黄金 */
    private Integer memberLevel;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
