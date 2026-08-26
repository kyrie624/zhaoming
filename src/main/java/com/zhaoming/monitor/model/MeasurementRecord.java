package com.zhaoming.monitor.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MeasurementRecord {
    private String deviceId;
    private String deviceName;
    private String floorName;
    private LocalDateTime collectedAt;
    private BigDecimal energyKwh;
    private BigDecimal currentA;
    private BigDecimal currentB;
    private BigDecimal currentC;
    private Boolean working;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
    public BigDecimal getEnergyKwh() { return energyKwh; }
    public void setEnergyKwh(BigDecimal energyKwh) { this.energyKwh = energyKwh; }
    public BigDecimal getCurrentA() { return currentA; }
    public void setCurrentA(BigDecimal currentA) { this.currentA = currentA; }
    public BigDecimal getCurrentB() { return currentB; }
    public void setCurrentB(BigDecimal currentB) { this.currentB = currentB; }
    public BigDecimal getCurrentC() { return currentC; }
    public void setCurrentC(BigDecimal currentC) { this.currentC = currentC; }
    public Boolean getWorking() { return working; }
    public void setWorking(Boolean working) { this.working = working; }
}
