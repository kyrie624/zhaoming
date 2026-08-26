package com.zhaoming.monitor.service;

import com.zhaoming.monitor.mapper.MeasurementMapper;
import com.zhaoming.monitor.mapper.WorkPeriodMapper;
import com.zhaoming.monitor.model.MeasurementRecord;
import com.zhaoming.monitor.model.DeviceRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class EnergyService {
    private final MeasurementMapper measurementMapper;
    private final WorkPeriodMapper workPeriodMapper;

    public EnergyService(MeasurementMapper measurementMapper, WorkPeriodMapper workPeriodMapper) {
        this.measurementMapper = measurementMapper;
        this.workPeriodMapper = workPeriodMapper;
    }

    public EnergyResult calculate(String deviceId, TimeRange range) {
        MeasurementRecord start = boundarySample(deviceId, range.getStart());
        MeasurementRecord end = boundarySample(deviceId, range.getEnd());
        if (start == null || end == null || start.getEnergyKwh() == null || end.getEnergyKwh() == null) {
            throw new IllegalArgumentException("指定时间范围没有足够的有功电能采样数据");
        }
        if (start.getCollectedAt().isAfter(end.getCollectedAt())) {
            throw new IllegalArgumentException("指定时间范围没有足够的有功电能采样数据");
        }
        long onlineSeconds = calculateOnlineSeconds(deviceId, range);
        return new EnergyResult(deviceId, range.getStart(), range.getEnd(),
                end.getDeviceName(), end.getFloorName(),
                start.getCollectedAt(), end.getCollectedAt(), start.getEnergyKwh(),
                end.getEnergyKwh(), end.getEnergyKwh().subtract(start.getEnergyKwh()),
                onlineSeconds, formatDuration(onlineSeconds));
    }

    private long calculateOnlineSeconds(String deviceId, TimeRange range) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        // 查询结束时间如果在未来，在线时长只能计算到当前时刻，不能把未来时间算进去。
        LocalDateTime effectiveEnd = range.getEnd().isAfter(now) ? now : range.getEnd();
        if (!effectiveEnd.isAfter(range.getStart())) {
            return 0L;
        }

        long seconds = 0L;
        for (com.zhaoming.monitor.model.WorkPeriodRecord period
                : workPeriodMapper.findOverlapping(deviceId, range.getStart(), effectiveEnd)) {
            LocalDateTime start = period.getStartAt().isBefore(range.getStart())
                    ? range.getStart() : period.getStartAt();
            LocalDateTime end = period.getEndAt() == null || period.getEndAt().isAfter(effectiveEnd)
                    ? effectiveEnd : period.getEndAt();
            if (end.isAfter(start)) {
                seconds += Duration.between(start, end).getSeconds();
            }
        }
        return seconds;
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    /** 按电表名称、区域筛选后，返回每个设备的电能差值。 */
    public List<EnergyResult> calculateByFilter(String deviceName, String floorName, TimeRange range) {
        List<EnergyResult> result = new ArrayList<>();
        for (DeviceRecord device : measurementMapper.findDevices(deviceName, floorName)) {
            try {
                result.add(calculate(device.getDeviceId(), range));
            } catch (IllegalArgumentException ignored) {
                // 该设备在所选时间范围内没有完整电能边界数据，不返回无效差值。
            }
        }
        return result;
    }

    /** 优先取边界前（含边界）的累计值；范围刚开始没有历史数据时取边界后的第一条。 */
    private MeasurementRecord boundarySample(String deviceId, java.time.LocalDateTime boundary) {
        MeasurementRecord before = measurementMapper.findLatestBefore(deviceId, boundary);
        return before != null ? before : measurementMapper.findEarliestAfter(deviceId, boundary);
    }

    public static class EnergyResult {
        private final String deviceId;
        private final java.time.LocalDateTime startTime;
        private final java.time.LocalDateTime endTime;
        private final String deviceName;
        private final String floorName;
        private final java.time.LocalDateTime startSampleTime;
        private final java.time.LocalDateTime endSampleTime;
        private final BigDecimal startEnergyKwh;
        private final BigDecimal endEnergyKwh;
        private final BigDecimal differenceKwh;
        private final long totalOnlineSeconds;
        private final String totalOnlineDuration;

        public EnergyResult(String deviceId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
                            String deviceName, String floorName,
                            java.time.LocalDateTime startSampleTime, java.time.LocalDateTime endSampleTime,
                            BigDecimal startEnergyKwh, BigDecimal endEnergyKwh, BigDecimal differenceKwh,
                            long totalOnlineSeconds, String totalOnlineDuration) {
            this.deviceId = deviceId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.deviceName = deviceName;
            this.floorName = floorName;
            this.startSampleTime = startSampleTime;
            this.endSampleTime = endSampleTime;
            this.startEnergyKwh = startEnergyKwh;
            this.endEnergyKwh = endEnergyKwh;
            this.differenceKwh = differenceKwh;
            this.totalOnlineSeconds = totalOnlineSeconds;
            this.totalOnlineDuration = totalOnlineDuration;
        }

        public String getDeviceId() { return deviceId; }
        public java.time.LocalDateTime getStartTime() { return startTime; }
        public java.time.LocalDateTime getEndTime() { return endTime; }
        public String getDeviceName() { return deviceName; }
        public String getFloorName() { return floorName; }
        public java.time.LocalDateTime getStartSampleTime() { return startSampleTime; }
        public java.time.LocalDateTime getEndSampleTime() { return endSampleTime; }
        public BigDecimal getStartEnergyKwh() { return startEnergyKwh; }
        public BigDecimal getEndEnergyKwh() { return endEnergyKwh; }
        public BigDecimal getDifferenceKwh() { return differenceKwh; }
        public long getTotalOnlineSeconds() { return totalOnlineSeconds; }
        public String getTotalOnlineDuration() { return totalOnlineDuration; }
    }
}
