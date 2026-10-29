package com.billiard.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 灯光设备实体
 */
@Data
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备平台标识 */
    private String deviceId;

    /** 设备平台：mock/涂鸦/机智云/ESP8266 */
    private String provider;

    private String name;

    /** 开关状态 0关 1开 */
    private Integer status;

    /** 绑定的球台ID */
    private Long bindTableId;

    private LocalDateTime createTime;
}
