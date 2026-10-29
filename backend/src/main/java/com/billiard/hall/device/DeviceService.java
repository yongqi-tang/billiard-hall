package com.billiard.hall.device;

/**
 * 灯光设备抽象接口
 * 开台亮灯、结账灭灯都通过它下发指令。
 * 提供 mock 实现先跑通全流程，后续可扩展涂鸦/机智云/ESP8266 实现。
 */
public interface DeviceService {

    /**
     * 控制灯开关
     * @param deviceId 设备ID
     * @param on true 开灯 / false 关灯
     */
    boolean turnOnOff(Long deviceId, boolean on);

    /**
     * 查询设备开关状态
     */
    boolean isOn(Long deviceId);
}
