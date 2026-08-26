package com.zhaoming.monitor.model;

/** 用于按前端筛选条件枚举设备。 */
public class DeviceRecord {
    private String deviceId;
    private String deviceName;
    private String floorName;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
}
