package com.zhaoming.monitor.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 按月统计所需的某个边界及其最近电能采样。 */
public class EnergyBoundaryRecord {
    private String boundaryKey;
    private LocalDateTime boundaryAt;
    private String deviceId;
    private LocalDateTime collectedAt;
    private BigDecimal energyKwh;

    public EnergyBoundaryRecord() {
    }

    public EnergyBoundaryRecord(String boundaryKey, LocalDateTime boundaryAt) {
        this.boundaryKey = boundaryKey;
        this.boundaryAt = boundaryAt;
    }

    public String getBoundaryKey() { return boundaryKey; }
    public void setBoundaryKey(String boundaryKey) { this.boundaryKey = boundaryKey; }
    public LocalDateTime getBoundaryAt() { return boundaryAt; }
    public void setBoundaryAt(LocalDateTime boundaryAt) { this.boundaryAt = boundaryAt; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
    public BigDecimal getEnergyKwh() { return energyKwh; }
    public void setEnergyKwh(BigDecimal energyKwh) { this.energyKwh = energyKwh; }
}
