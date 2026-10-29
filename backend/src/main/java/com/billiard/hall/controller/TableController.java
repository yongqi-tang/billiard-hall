package com.billiard.hall.controller;

import com.billiard.hall.common.Result;
import com.billiard.hall.entity.BilliardTable;
import com.billiard.hall.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 球台接口
 */
@RestController
@RequestMapping("/api/table")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    /**
     * 球台列表 + 实时状态（小程序首页用）
     */
    @GetMapping("/list")
    public Result<List<BilliardTable>> list() {
        return Result.ok(tableService.listAll());
    }
}
