package com.zhaoming.monitor.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 一次外部接口采样后提取出的设备关键测量值。 */
public class MeterMeasurement {

    private final String deviceId;
    private final String deviceName;
    private final String floorName;
    private final LocalDateTime collectedAt;
    private final BigDecimal energyKwh;
    private final BigDecimal currentA;
    private final BigDecimal currentB;
    private final BigDecimal currentC;
    private final boolean working;

    public MeterMeasurement(String deviceId, String deviceName, String floorName, LocalDateTime collectedAt,
                            BigDecimal energyKwh, BigDecimal currentA, BigDecimal currentB,
                            BigDecimal currentC, boolean working) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.floorName = floorName;
        this.collectedAt = collectedAt;
        this.energyKwh = energyKwh;
        this.currentA = currentA;
        this.currentB = currentB;
        this.currentC = currentC;
        this.working = working;
    }

    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public String getFloorName() { return floorName; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public BigDecimal getEnergyKwh() { return energyKwh; }
    public BigDecimal getCurrentA() { return currentA; }
    public BigDecimal getCurrentB() { return currentB; }
    public BigDecimal getCurrentC() { return currentC; }
    public boolean isWorking() { return working; }
}
