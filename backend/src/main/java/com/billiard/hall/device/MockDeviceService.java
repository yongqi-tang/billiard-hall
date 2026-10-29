package com.billiard.hall.device;

import com.billiard.hall.entity.Device;
import com.billiard.hall.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟灯光设备（开发阶段用）
 * 等接入真实硬件时，新增实现类并加 @Primary 替换即可。
 *
 * 开台亮灯/结账灭灯时：内存记录 + 同步 device 表 status 字段，
 * 这样管理后台 /api/device/list 能查到实时灯光状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockDeviceService implements DeviceService {

    private final DeviceMapper deviceMapper;

    /** 内存中记录设备开关状态（真实版由设备平台返回） */
    private final Map<Long, Boolean> statusMap = new ConcurrentHashMap<>();

    @Override
    public boolean turnOnOff(Long deviceId, boolean on) {
        statusMap.put(deviceId, on);
        // 同步数据库，让管理后台能看到实时状态
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device != null) {
                device.setStatus(on ? 1 : 0);
                deviceMapper.updateById(device);
            }
        } catch (Exception e) {
            log.warn("[mock设备] 同步设备状态失败 deviceId={}", deviceId, e);
        }
        log.info("[mock设备] {}灯, deviceId={}", on ? "开" : "关", deviceId);
        return true;
    }

    @Override
    public boolean isOn(Long deviceId) {
        return Boolean.TRUE.equals(statusMap.getOrDefault(deviceId, false));
    }
}
