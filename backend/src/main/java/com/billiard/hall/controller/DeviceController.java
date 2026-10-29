package com.billiard.hall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.billiard.hall.common.Result;
import com.billiard.hall.device.DeviceService;
import com.billiard.hall.entity.Device;
import com.billiard.hall.mapper.DeviceMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备接口
 */
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceMapper deviceMapper;
    private final DeviceService deviceService;

    /** 设备列表 */
    @GetMapping("/list")
    public Result<List<Device>> list() {
        return Result.ok(deviceMapper.selectList(
                new LambdaQueryWrapper<Device>().orderByAsc(Device::getId)));
    }

    /** 手动控制灯光开关（管理后台用） */
    @PostMapping("/control")
    public Result<Boolean> control(@RequestBody ControlReq req) {
        boolean ok = deviceService.turnOnOff(req.getDeviceId(), req.getOn());
        Device device = deviceMapper.selectById(req.getDeviceId());
        if (device != null) {
            device.setStatus(req.getOn() ? 1 : 0);
            deviceMapper.updateById(device);
        }
        return Result.ok(ok);
    }

    @Data
    public static class ControlReq {
        private Long deviceId;
        private Boolean on;
    }
}
