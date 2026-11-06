package com.billiard.hall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.billiard.hall.entity.BilliardTable;
import com.billiard.hall.entity.Reservation;
import com.billiard.hall.mapper.BilliardTableMapper;
import com.billiard.hall.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 球台业务
 */
@Service
@RequiredArgsConstructor
public class TableService {

    private final BilliardTableMapper tableMapper;
    private final ReservationMapper reservationMapper;

    /**
     * 获取球台列表及实时状态（含预约标记）
     */
    public List<BilliardTable> listAll() {
        List<BilliardTable> tables = tableMapper.selectList(
                new LambdaQueryWrapper<BilliardTable>().orderByAsc(BilliardTable::getId));
        LocalDate today = LocalDate.now();
        for (BilliardTable t : tables) {
            Long count = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getTableId, t.getId())
                    .ge(Reservation::getReserveDate, today)
                    .eq(Reservation::getStatus, 0));
            t.setReserved(count > 0);
            t.setReservedCount(count.intValue());
        }
        return tables;
    }
}
