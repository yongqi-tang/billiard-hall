package com.billiard.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约实体
 */
@Data
@TableName("billiard_reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约单号 */
    private String reservationNo;

    private Long userId;

    private Long tableId;

    /** 预约日期 */
    private LocalDate reserveDate;

    /** 预约开始时间 */
    private LocalTime startTime;

    /** 预约结束时间 */
    private LocalTime endTime;

    /** 状态 0待使用 1已开台 2已完成 3已取消 4超时释放 */
    private Integer status;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
