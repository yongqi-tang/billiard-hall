package com.billiard.hall.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.billiard.hall.common.BusinessException;
import com.billiard.hall.entity.BilliardTable;
import com.billiard.hall.entity.Reservation;
import com.billiard.hall.mapper.BilliardTableMapper;
import com.billiard.hall.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 预约业务：在线预约 / 取消 / 到期开台 / 超时释放
 *
 * 并发处理：
 *  - 同一球台同一时段冲突检测采用“先查后插 + 数据库唯一约束兜底”
 *  - 开台时校验预约归属与时段，避免跨时段使用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    /** 预约允许提前多少天 */
    private static final int MAX_ADVANCE_DAYS = 7;

    /** 开台最晚可迟到分钟数，超过则视为爽约自动释放 */
    private static final int CHECKIN_GRACE_MINUTES = 30;

    private final ReservationMapper reservationMapper;
    private final BilliardTableMapper tableMapper;

    /**
     * 创建预约：校验球台空闲、时段不冲突、用户无重复时段预约
     */
    @Transactional(rollbackFor = Exception.class)
    public Reservation create(Long userId, Long tableId, LocalDate date,
                              LocalTime startTime, LocalTime endTime, String remark) {
        // 参数校验
        if (userId == null || tableId == null || date == null || startTime == null || endTime == null) {
            throw new BusinessException("预约参数不完整");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new BusinessException("预约日期需在今天起 " + MAX_ADVANCE_DAYS + " 天内");
        }

        // 球台存在且非维修
        BilliardTable table = tableMapper.selectById(tableId);
        if (table == null || table.getDeleted() != null && table.getDeleted() == 1) {
            throw new BusinessException("球台不存在");
        }
        if (table.getStatus() != null && table.getStatus() == 2) {
            throw new BusinessException("该球台正在维修，暂不可预约");
        }

        // 同一用户同一时段不能重复预约（避免刷单）
        Long dupUser = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getReserveDate, date)
                .in(Reservation::getStatus, 0, 1)
                .and(w -> w
                        .lt(Reservation::getStartTime, endTime)
                        .gt(Reservation::getEndTime, startTime)));
        if (dupUser > 0) {
            throw new BusinessException("您在该时段已有预约，请勿重复预约");
        }

        // 球台该时段冲突检测
        Long conflict = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getTableId, tableId)
                .eq(Reservation::getReserveDate, date)
                .in(Reservation::getStatus, 0, 1)
                .and(w -> w
                        .lt(Reservation::getStartTime, endTime)
                        .gt(Reservation::getEndTime, startTime)));
        if (conflict > 0) {
            throw new BusinessException("该球台此时间段已被预约，请选择其他时段");
        }

        // 该球台当前正被占用（进行中订单）
        if (table.getStatus() != null && table.getStatus() == 1) {
            throw new BusinessException("该球台当前正在使用中，暂不可预约");
        }

        Reservation r = new Reservation();
        r.setReservationNo("RS" + IdUtil.getSnowflakeNextIdStr());
        r.setUserId(userId);
        r.setTableId(tableId);
        r.setReserveDate(date);
        r.setStartTime(startTime);
        r.setEndTime(endTime);
        r.setStatus(0);
        r.setRemark(remark);
        reservationMapper.insert(r);

        log.info("预约成功 reservationNo={} userId={} tableId={} {} {}-{}",
                r.getReservationNo(), userId, tableId, date, startTime, endTime);
        return r;
    }

    /**
     * 我的预约列表（待使用在前）
     */
    public List<Reservation> myReservations(Long userId) {
        return reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .orderByAsc(Reservation::getStatus)
                .orderByDesc(Reservation::getReserveDate)
                .orderByAsc(Reservation::getStartTime));
    }

    /**
     * 全部预约（管理后台）
     */
    public List<Reservation> allReservations() {
        return reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .orderByDesc(Reservation::getId));
    }

    /**
     * 取消预约：仅待使用状态可取消
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long reservationId, Long userId) {
        Reservation r = getOwned(reservationId, userId);
        if (r.getStatus() != 0) {
            throw new BusinessException("当前状态不可取消");
        }
        r.setStatus(3);
        reservationMapper.updateById(r);
        log.info("取消预约 id={}", reservationId);
    }

    /**
     * 到店开台：把预约转为进行中订单（衔接 OrderService.open 前校验）
     *  - 预约到时间即可开台；未到时间提前到店不允许
     *  - 超时未到店由定时任务自动释放
     */
    @Transactional(rollbackFor = Exception.class)
    public Reservation checkin(Long reservationId, Long userId, Long tableId) {
        Reservation r = getOwned(reservationId, userId);
        if (r.getStatus() != 0) {
            throw new BusinessException("该预约已不可开台");
        }
        if (!r.getTableId().equals(tableId)) {
            throw new BusinessException("预约球台与扫码球台不一致");
        }
        // 开台时间判断：可提前 10 分钟内，不可晚到超过宽限
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = LocalDateTime.of(r.getReserveDate(), r.getStartTime());
        if (now.isBefore(start.minusMinutes(10))) {
            throw new BusinessException("未到预约开始时间，请稍后再来");
        }
        if (now.isAfter(start.plusMinutes(CHECKIN_GRACE_MINUTES))) {
            // 已超时，标记释放（实际由定时任务兜底）
            r.setStatus(4);
            reservationMapper.updateById(r);
            throw new BusinessException("已超过预约开台时间，预约已释放，请直接扫码开台");
        }

        // 球台当前不可用（被其他人抢先使用）则拒绝
        BilliardTable table = tableMapper.selectById(tableId);
        if (table == null || table.getStatus() != null && table.getStatus() != 0) {
            throw new BusinessException("该球台当前不可用");
        }

        // 标记已开台（真正开台动作由 OrderService.open 完成）
        r.setStatus(1);
        reservationMapper.updateById(r);
        log.info("预约到店开台 reservationId={}", reservationId);
        return r;
    }

    /**
     * 定时任务：过期未开台的预约自动释放
     *  - 预约结束时间已过且未开台 -> 已完成
     *  - 超过开台宽限时间仍未开台 -> 超时释放
     */
    @Transactional(rollbackFor = Exception.class)
    public int releaseTimeoutReservations() {
        LocalDateTime now = LocalDateTime.now();
        int changed = 0;

        // 待使用的预约
        List<Reservation> pending = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getStatus, 0));
        for (Reservation r : pending) {
            LocalDateTime start = LocalDateTime.of(r.getReserveDate(), r.getStartTime());
            LocalDateTime end = LocalDateTime.of(r.getReserveDate(), r.getEndTime());
            if (now.isAfter(end)) {
                // 预约时段已过但没来 -> 已完成（未使用）
                r.setStatus(2);
                reservationMapper.updateById(r);
                changed++;
            } else if (now.isAfter(start.plusMinutes(CHECKIN_GRACE_MINUTES))) {
                // 开台宽限已过 -> 超时释放
                r.setStatus(4);
                reservationMapper.updateById(r);
                changed++;
            }
        }
        if (changed > 0) {
            log.info("预约超时/过期释放 {} 条", changed);
        }
        return changed;
    }

    /**
     * 管理后台取消预约（任意待使用预约）
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminCancel(Long reservationId) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) {
            throw new BusinessException("预约不存在");
        }
        if (r.getStatus() != 0) {
            throw new BusinessException("当前状态不可取消");
        }
        r.setStatus(3);
        reservationMapper.updateById(r);
    }

    // ================== 私有方法 ==================

    private Reservation getOwned(Long reservationId, Long userId) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null || !r.getUserId().equals(userId)) {
            throw new BusinessException("预约不存在");
        }
        return r;
    }
}
