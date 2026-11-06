package com.billiard.hall.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.billiard.hall.common.BusinessException;
import com.billiard.hall.config.BilliardProperties;
import com.billiard.hall.device.DeviceService;
import com.billiard.hall.entity.BilliardOrder;
import com.billiard.hall.entity.BilliardTable;
import com.billiard.hall.entity.Device;
import com.billiard.hall.entity.RechargeRecord;
import com.billiard.hall.entity.Reservation;
import com.billiard.hall.entity.User;
import com.billiard.hall.mapper.BilliardOrderMapper;
import com.billiard.hall.mapper.BilliardTableMapper;
import com.billiard.hall.mapper.DeviceMapper;
import com.billiard.hall.mapper.RechargeRecordMapper;
import com.billiard.hall.mapper.ReservationMapper;
import com.billiard.hall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单核心业务：开台 / 续时 / 结账 / 计费
 *
 * 计时策略：
 *  - 后端不依赖前端倒计时，实时按 start_time 计算已用分钟
 *  - Redis 未部署时用内存 Map 兜底（本项目自带内存实现，接口对上层透明）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final BilliardOrderMapper orderMapper;
    private final BilliardTableMapper tableMapper;
    private final UserMapper userMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final ReservationMapper reservationMapper;
    private final DeviceMapper deviceMapper;
    private final DeviceService deviceService;
    private final BilliardProperties props;

    /** 欠费标记：tableId -> 欠费开始时间 */
    private final Map<Long, LocalDateTime> debtMap = new ConcurrentHashMap<>();

    /**
     * 扫码开台
     */
    @Transactional(rollbackFor = Exception.class)
    public BilliardOrder open(Long userId, Long tableId) {
        BilliardTable table = getTable(tableId);
        // 球台可用性：空闲(0)可直接开台；预约(3)须用户有当前时段预约才允许
        if (table.getStatus() != 0 && table.getStatus() != 3) {
            throw new BusinessException("该球台当前不可用");
        }
        // 同一用户不能同时开两台
        Long active = orderMapper.selectCount(new LambdaQueryWrapper<BilliardOrder>()
                .eq(BilliardOrder::getUserId, userId)
                .eq(BilliardOrder::getStatus, 0));
        if (active > 0) {
            throw new BusinessException("您有进行中的球局，请先结账");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在，请先登录");
        }
        // 余额校验：至少够起步价
        if (user.getBalance().compareTo(props.getStartPrice()) < 0) {
            throw new BusinessException("余额不足起步价，请先充值");
        }

        // 预约核销：用户在当前球台本时段有待使用预约则自动核销（无则忽略）
        checkinReservationIfAny(userId, tableId);
        // 若球台仍处于预约态但用户无本时段有效预约（说明被他人预约），拒绝
        BilliardTable fresh = tableMapper.selectById(tableId);
        if (fresh.getStatus() == 3) {
            throw new BusinessException("该球台已被预约，您没有本时段的有效预约");
        }

        // 扣起步价（先付起步，多退少补在结账时算）
        BigDecimal start = props.getStartPrice();
        user.setBalance(user.getBalance().subtract(start));
        userMapper.updateById(user);
        insertRecharge(userId, start.negate(), 2, null);

        // 建订单
        BilliardOrder order = new BilliardOrder();
        order.setOrderNo("BH" + IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);
        order.setTableId(tableId);
        order.setStartTime(LocalDateTime.now());
        order.setDurationMin(0);
        order.setAmount(start);
        order.setStatus(0);
        order.setPayStatus(0);
        orderMapper.insert(order);

        // 球台占位 + 开灯
        table.setStatus(1);
        tableMapper.updateById(table);
        turnTableLight(table, true);

        log.info("开台成功 orderNo={} tableId={} userId={}", order.getOrderNo(), tableId, userId);
        return order;
    }

    /**
     * 续时：继续扣钱（按起步价相同单价逻辑简化：扣一个起步价的时长）
     */
    @Transactional(rollbackFor = Exception.class)
    public BilliardOrder extend(Long orderId, Long userId) {
        BilliardOrder order = getActiveOrder(orderId, userId);
        User user = userMapper.selectById(userId);
        BilliardTable table = getTable(order.getTableId());

        BigDecimal cost = props.getStartPrice();
        if (user.getBalance().compareTo(cost) < 0) {
            throw new BusinessException("余额不足，无法续时");
        }
        user.setBalance(user.getBalance().subtract(cost));
        userMapper.updateById(user);
        insertRecharge(userId, cost.negate(), 2, order.getId());

        // 续时即重新计算：结账按真实时长，所以这里只记录流水，不改 start_time
        log.info("续时成功 orderNo={} +{}元", order.getOrderNo(), cost);
        return order;
    }

    /**
     * 结账：按真实时长计算金额，扣款/退款，灭灯
     */
    @Transactional(rollbackFor = Exception.class)
    public BilliardOrder settle(Long orderId, Long userId) {
        BilliardOrder order = getActiveOrder(orderId, userId);
        User user = userMapper.selectById(userId);
        BilliardTable table = getTable(order.getTableId());

        // 计算费用
        long minutes = usedMinutes(order);
        BigDecimal total = calcAmount(minutes, table.getPriceStart(), table.getPriceHour());
        BigDecimal start = order.getAmount(); // 开台时已扣起步价
        BigDecimal diff = total.subtract(start);

        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            // 补收差额
            user.setBalance(user.getBalance().subtract(diff));
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            // 未满起步价，退回多扣
            user.setBalance(user.getBalance().add(diff.negate()));
        }
        userMapper.updateById(user);
        insertRecharge(userId, diff.negate(), 2, order.getId());

        // 结账
        order.setEndTime(LocalDateTime.now());
        order.setDurationMin((int) minutes);
        order.setAmount(total);
        order.setStatus(1);
        order.setPayStatus(1);
        orderMapper.updateById(order);

        // 球台释放 + 灭灯
        table.setStatus(0);
        tableMapper.updateById(table);
        turnTableLight(table, false);
        debtMap.remove(table.getId());

        log.info("结账成功 orderNo={} 时长={}min 金额={}", order.getOrderNo(), minutes, total);
        return order;
    }

    /**
     * 取消订单（未超时可取消，退回已扣费用）
     */
    @Transactional(rollbackFor = Exception.class)
    public BilliardOrder cancel(Long orderId, Long userId) {
        BilliardOrder order = getActiveOrder(orderId, userId);
        User user = userMapper.selectById(userId);
        BilliardTable table = getTable(order.getTableId());

        // 退回起步价
        user.setBalance(user.getBalance().add(order.getAmount()));
        userMapper.updateById(user);
        insertRecharge(userId, order.getAmount(), 3, order.getId());

        order.setStatus(2);
        order.setEndTime(LocalDateTime.now());
        order.setAmount(BigDecimal.ZERO);
        orderMapper.updateById(order);

        table.setStatus(0);
        tableMapper.updateById(table);
        turnTableLight(table, false);
        debtMap.remove(table.getId());
        return order;
    }

    /**
     * 用户当前进行中的订单（含实时已用分钟和预估金额）
     */
    public Map<String, Object> current(Long userId) {
        BilliardOrder order = orderMapper.selectOne(new LambdaQueryWrapper<BilliardOrder>()
                .eq(BilliardOrder::getUserId, userId)
                .eq(BilliardOrder::getStatus, 0)
                .last("limit 1"));
        if (order == null) {
            return null;
        }
        BilliardTable table = getTable(order.getTableId());
        long minutes = usedMinutes(order);
        BigDecimal total = calcAmount(minutes, table.getPriceStart(), table.getPriceHour());

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("order", order);
        data.put("table", table);
        data.put("usedMinutes", minutes);
        data.put("estimatedAmount", total);
        data.put("debt", debtMap.containsKey(table.getId()));
        return data;
    }

    /**
     * 我的订单历史
     */
    public List<BilliardOrder> myOrders(Long userId) {
        return orderMapper.selectList(new LambdaQueryWrapper<BilliardOrder>()
                .eq(BilliardOrder::getUserId, userId)
                .orderByDesc(BilliardOrder::getId));
    }

    /**
     * 定时任务：欠费/超时检查（见 OrderScheduleTask）
     *  - 进行中订单余额不足起步价单价持续 30 分钟 -> 强制结账
     */
    public void checkTimeout() {
        List<BilliardOrder> activeOrders = orderMapper.selectList(
                new LambdaQueryWrapper<BilliardOrder>().eq(BilliardOrder::getStatus, 0));
        for (BilliardOrder order : activeOrders) {
            BilliardTable table = getTable(order.getTableId());
            User user = userMapper.selectById(order.getUserId());
            if (user == null) continue;

            // 余额低于起步单价，标记欠费
            if (user.getBalance().compareTo(table.getPriceStart()) < 0) {
                debtMap.putIfAbsent(table.getId(), LocalDateTime.now());
                // 欠费超时，强制结账
                if (Duration.between(debtMap.get(table.getId()), LocalDateTime.now())
                        .toMinutes() >= props.getDebtTimeoutMinutes()) {
                    log.warn("欠费超时强制结账 orderNo={}", order.getOrderNo());
                    forceSettle(order, table, user);
                }
            } else {
                debtMap.remove(table.getId());
            }
        }
    }

    /**
     * 强制结账（余额不足时按已用时长结算，允许余额扣成负数）
     */
    @Transactional(rollbackFor = Exception.class)
    public void forceSettle(BilliardOrder order, BilliardTable table, User user) {
        long minutes = usedMinutes(order);
        BigDecimal total = calcAmount(minutes, table.getPriceStart(), table.getPriceHour());
        BigDecimal start = order.getAmount();
        BigDecimal diff = total.subtract(start);
        user.setBalance(user.getBalance().subtract(diff));
        userMapper.updateById(user);
        insertRecharge(user.getId(), diff.negate(), 2, order.getId());

        order.setEndTime(LocalDateTime.now());
        order.setDurationMin((int) minutes);
        order.setAmount(total);
        order.setStatus(3); // 欠费结账
        order.setPayStatus(1);
        orderMapper.updateById(order);

        table.setStatus(0);
        tableMapper.updateById(table);
        turnTableLight(table, false);
        debtMap.remove(table.getId());
    }

    // ================== 私有方法 ==================

    /**
     * 预约核销：查询用户在当前球台、当前时段的待使用预约，有则核销为已开台（无则忽略）
     */
    private void checkinReservationIfAny(Long userId, Long tableId) {
        LocalDateTime now = LocalDateTime.now();
        Reservation r = reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getTableId, tableId)
                .eq(Reservation::getReserveDate, now.toLocalDate())
                .eq(Reservation::getStatus, 0)
                .le(Reservation::getStartTime, now.toLocalTime())
                .ge(Reservation::getEndTime, now.toLocalTime())
                .last("limit 1"));
        if (r != null) {
            r.setStatus(1);
            reservationMapper.updateById(r);
            log.info("预约核销成功 reservationNo={}", r.getReservationNo());
        }
    }

    private BilliardOrder getActiveOrder(Long orderId, Long userId) {
        BilliardOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId) || order.getStatus() != 0) {
            throw new BusinessException("订单不存在或已结束");
        }
        return order;
    }

    private BilliardTable getTable(Long tableId) {
        BilliardTable table = tableMapper.selectById(tableId);
        if (table == null) {
            throw new BusinessException("球台不存在");
        }
        return table;
    }

    private long usedMinutes(BilliardOrder order) {
        Duration d = Duration.between(order.getStartTime(), LocalDateTime.now());
        return Math.max(1, d.toMinutes());
    }

    /**
     * 计费：先扣起步价对应时长，超出按分钟单价（不足1分钟按1分钟）
     */
    public BigDecimal calcAmount(long minutes, BigDecimal priceStart, BigDecimal priceHour) {
        BigDecimal perMinute = priceHour.divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        if (minutes <= props.getStartPriceMinutes()) {
            return priceStart;
        }
        long extra = minutes - props.getStartPriceMinutes();
        return priceStart.add(perMinute.multiply(BigDecimal.valueOf(extra))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 控制球台灯（通过绑定的设备）
     */
    private void turnTableLight(BilliardTable table, boolean on) {
        if (table.getDeviceId() != null) {
            deviceService.turnOnOff(table.getDeviceId(), on);
        }
    }

    private void insertRecharge(Long userId, BigDecimal amount, int type, Long orderId) {
        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setType(type);
        record.setOrderId(orderId);
        record.setCreateTime(LocalDateTime.now());
        rechargeRecordMapper.insert(record);
    }
}
