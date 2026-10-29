package com.billiard.hall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.billiard.hall.entity.BilliardTable;
import com.billiard.hall.mapper.BilliardTableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 球台业务
 */
@Service
@RequiredArgsConstructor
public class TableService {

    private final BilliardTableMapper tableMapper;

    /**
     * 获取球台列表及实时状态
     */
    public List<BilliardTable> listAll() {
        return tableMapper.selectList(
                new LambdaQueryWrapper<BilliardTable>().orderByAsc(BilliardTable::getId));
    }
}
