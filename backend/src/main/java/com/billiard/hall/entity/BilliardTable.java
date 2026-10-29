package com.billiard.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 球台实体
 */
@Data
@TableName("billiard_table")
public class BilliardTable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 球台名称，如 A区1号台 */
    private String name;

    /** 位置描述 */
    private String location;

    /** 状态 0空闲 1使用中 2维修 3预约 */
    private Integer status;

    /** 起步价 */
    private BigDecimal priceStart;

    /** 每小时价格 */
    private BigDecimal priceHour;

    /** 绑定的灯光设备ID */
    private Long deviceId;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
