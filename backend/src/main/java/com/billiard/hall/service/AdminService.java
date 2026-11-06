package com.billiard.hall.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.billiard.hall.common.BusinessException;
import com.billiard.hall.entity.BilliardOrder;
import com.billiard.hall.entity.BilliardTable;
import com.billiard.hall.entity.RechargeRecord;
import com.billiard.hall.entity.Reservation;
import com.billiard.hall.entity.User;
import com.billiard.hall.mapper.BilliardOrderMapper;
import com.billiard.hall.mapper.BilliardTableMapper;
import com.billiard.hall.mapper.RechargeRecordMapper;
import com.billiard.hall.mapper.ReservationMapper;
import com.billiard.hall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台业务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final BilliardTableMapper tableMapper;
    private final BilliardOrderMapper orderMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final ReservationMapper reservationMapper;

    /** 管理员账号密码（application.yml 可配） */
    @Value("${billiard.admin.username:admin}")
    private String adminUsername;

    @Value("${billiard.admin.password:admin123}")
    private String adminPassword;

    /**
     * 管理员登录（简化：固定账号）
     */
    public boolean login(String username, String password) {
        return StrUtil.equals(username, adminUsername)
                && StrUtil.equals(password, adminPassword);
    }

    /**
     * 球台增删改
     */
    public BilliardTable saveTable(BilliardTable table) {
        if (table.getId() == null) {
            tableMapper.insert(table);
        } else {
            tableMapper.updateById(table);
        }
        return table;
    }

    public void deleteTable(Long id) {
        tableMapper.deleteById(id);
    }

    /**
     * 全部订单
     */
    public List<BilliardOrder> allOrders() {
        return orderMapper.selectList(
                new LambdaQueryWrapper<BilliardOrder>().orderByDesc(BilliardOrder::getId));
    }

    /**
     * 全部用户
     */
    public List<User> allUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getId));
    }

    /**
     * 营收统计
     */
    public Map<String, Object> statistics() {
        List<BilliardOrder> finished = orderMapper.selectList(
                new LambdaQueryWrapper<BilliardOrder>().in(BilliardOrder::getStatus, 1, 3));
        BigDecimal totalIncome = finished.stream()
                .map(BilliardOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 今日营收
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        BigDecimal todayIncome = finished.stream()
                .filter(o -> o.getEndTime() != null && !o.getEndTime().isBefore(todayStart))
                .map(BilliardOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 进行中的订单数
        Long activeCount = orderMapper.selectCount(
                new LambdaQueryWrapper<BilliardOrder>().eq(BilliardOrder::getStatus, 0));

        // 总充值
        List<RechargeRecord> recharges = rechargeRecordMapper.selectList(
                new LambdaQueryWrapper<RechargeRecord>().eq(RechargeRecord::getType, 1));
        BigDecimal totalRecharge = recharges.stream()
                .map(RechargeRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new HashMap<>();
        data.put("totalIncome", totalIncome);
        data.put("todayIncome", todayIncome);
        data.put("totalOrders", finished.size());
        data.put("activeOrders", activeCount);
        data.put("totalRecharge", totalRecharge);
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalTables", tableMapper.selectCount(null));
        data.put("pendingReservations", reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getStatus, 0)));
        return data;
    }

    /**
     * 给用户充值（管理后台操作）
     */
    public User adminRecharge(Long userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setBalance(user.getBalance().add(amount));
        userMapper.updateById(user);
        return user;
    }
}
