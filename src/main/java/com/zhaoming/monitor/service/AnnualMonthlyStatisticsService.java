package com.zhaoming.monitor.service;

import com.zhaoming.monitor.mapper.MeasurementMapper;
import com.zhaoming.monitor.mapper.WorkPeriodMapper;
import com.zhaoming.monitor.model.DeviceRecord;
import com.zhaoming.monitor.model.EnergyBoundaryRecord;
import com.zhaoming.monitor.model.WorkPeriodRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 为“当年按月统计”看板组装每台设备截至当月的指标。 */
@Service
public class AnnualMonthlyStatisticsService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final MeasurementMapper measurementMapper;
    private final WorkPeriodMapper workPeriodMapper;

    public AnnualMonthlyStatisticsService(MeasurementMapper measurementMapper, WorkPeriodMapper workPeriodMapper) {
        this.measurementMapper = measurementMapper;
        this.workPeriodMapper = workPeriodMapper;
    }

    @Transactional(readOnly = true)
    public AnnualStatistics calculate(Integer requestedYear, String deviceName, String floorName) {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE).withNano(0);
        int year = requestedYear == null ? now.getYear() : requestedYear;
        return calculate(year, deviceName, floorName, now);
    }

    AnnualStatistics calculate(int year, String deviceName, String floorName, LocalDateTime now) {
        validateYear(year);
        int lastMonth = lastMonthToReturn(year, now);

        List<MonthHeader> monthHeaders = new ArrayList<>();
        for (int month = 1; month <= lastMonth; month++) {
            monthHeaders.add(new MonthHeader(month, year + "年" + month + "月"));
        }

        List<DeviceRecord> deviceRecords = measurementMapper.findDevices(deviceName, floorName);
        if (deviceRecords.isEmpty() || lastMonth == 0) {
            return new AnnualStatistics(year, now, monthHeaders, toEmptyDeviceStatistics(deviceRecords));
        }

        List<String> deviceIds = new ArrayList<>();
        for (DeviceRecord device : deviceRecords) {
            deviceIds.add(device.getDeviceId());
        }

        List<EnergyBoundaryRecord> boundaryPoints = createBoundaryPoints(year, lastMonth, now);
        Map<String, Map<String, EnergyBoundaryRecord>> boundariesByDevice = groupBoundaries(
                measurementMapper.findEnergyBoundaries(deviceIds, boundaryPoints));

        LocalDateTime statisticsStart = YearMonth.of(year, 1).atDay(1).atStartOfDay();
        LocalDateTime statisticsEnd = boundaryPoints.get(boundaryPoints.size() - 1).getBoundaryAt();
        Map<String, List<WorkPeriodRecord>> periodsByDevice = groupPeriods(
                workPeriodMapper.findOverlappingForDevices(deviceIds, statisticsStart, statisticsEnd));

        List<DeviceStatistics> devices = new ArrayList<>();
        for (DeviceRecord device : deviceRecords) {
            devices.add(calculateDevice(year, lastMonth, device,
                    boundariesByDevice.get(device.getDeviceId()),
                    periodsByDevice.get(device.getDeviceId()), now));
        }
        return new AnnualStatistics(year, now, monthHeaders, devices);
    }

    private DeviceStatistics calculateDevice(int year, int lastMonth, DeviceRecord device,
                                             Map<String, EnergyBoundaryRecord> boundaries,
                                             List<WorkPeriodRecord> workPeriods,
                                             LocalDateTime now) {
        List<MonthlyStatistic> months = new ArrayList<>();
        BigDecimal totalDifferenceKwh = BigDecimal.ZERO;
        long totalOnlineSeconds = 0L;
        Map<String, EnergyBoundaryRecord> deviceBoundaries = boundaries == null
                ? Collections.emptyMap() : boundaries;
        List<WorkPeriodRecord> devicePeriods = workPeriods == null
                ? Collections.emptyList() : workPeriods;

        for (int month = 1; month <= lastMonth; month++) {
            LocalDateTime monthStart = YearMonth.of(year, month).atDay(1).atStartOfDay();
            LocalDateTime naturalEnd = monthStart.plusMonths(1);
            LocalDateTime effectiveEnd = naturalEnd.isAfter(now) ? now : naturalEnd;
            EnergyBoundaryRecord start = deviceBoundaries.get(boundaryKey(month - 1));
            EnergyBoundaryRecord end = deviceBoundaries.get(boundaryKey(month));
            if (!hasUsableEnergyBoundary(start, end)) {
                months.add(MonthlyStatistic.empty(month));
                continue;
            }

            BigDecimal differenceKwh = end.getEnergyKwh().subtract(start.getEnergyKwh());
            long onlineSeconds = calculateOnlineSeconds(devicePeriods, monthStart, effectiveEnd);
            months.add(MonthlyStatistic.of(month, differenceKwh, onlineSeconds));
            totalDifferenceKwh = totalDifferenceKwh.add(differenceKwh);
            totalOnlineSeconds += onlineSeconds;
        }

        return new DeviceStatistics(device.getDeviceId(), device.getDeviceName(), device.getFloorName(),
                totalDifferenceKwh, totalOnlineSeconds, formatDuration(totalOnlineSeconds), months);
    }

    private List<EnergyBoundaryRecord> createBoundaryPoints(int year, int lastMonth, LocalDateTime now) {
        List<EnergyBoundaryRecord> result = new ArrayList<>();
        LocalDateTime yearStart = YearMonth.of(year, 1).atDay(1).atStartOfDay();
        for (int index = 0; index <= lastMonth; index++) {
            LocalDateTime boundary = yearStart.plusMonths(index);
            if (index == lastMonth && year == now.getYear()) {
                boundary = now;
            }
            result.add(new EnergyBoundaryRecord(boundaryKey(index), boundary));
        }
        return result;
    }

    private Map<String, Map<String, EnergyBoundaryRecord>> groupBoundaries(
            List<EnergyBoundaryRecord> boundaries) {
        Map<String, Map<String, EnergyBoundaryRecord>> result = new HashMap<>();
        for (EnergyBoundaryRecord boundary : boundaries) {
            result.computeIfAbsent(boundary.getDeviceId(), ignored -> new HashMap<>())
                    .put(boundary.getBoundaryKey(), boundary);
        }
        return result;
    }

    private Map<String, List<WorkPeriodRecord>> groupPeriods(List<WorkPeriodRecord> workPeriods) {
        Map<String, List<WorkPeriodRecord>> result = new HashMap<>();
        for (WorkPeriodRecord period : workPeriods) {
            result.computeIfAbsent(period.getDeviceId(), ignored -> new ArrayList<>()).add(period);
        }
        return result;
    }

    private boolean hasUsableEnergyBoundary(EnergyBoundaryRecord start, EnergyBoundaryRecord end) {
        return start != null && end != null
                && start.getEnergyKwh() != null && end.getEnergyKwh() != null
                && start.getCollectedAt() != null && end.getCollectedAt() != null
                && !start.getCollectedAt().isAfter(end.getCollectedAt());
    }

    private long calculateOnlineSeconds(List<WorkPeriodRecord> periods,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        long seconds = 0L;
        for (WorkPeriodRecord period : periods) {
            LocalDateTime start = period.getStartAt().isBefore(rangeStart)
                    ? rangeStart : period.getStartAt();
            LocalDateTime end = period.getEndAt() == null || period.getEndAt().isAfter(rangeEnd)
                    ? rangeEnd : period.getEndAt();
            if (end.isAfter(start)) {
                seconds += Duration.between(start, end).getSeconds();
            }
        }
        return seconds;
    }

    private List<DeviceStatistics> toEmptyDeviceStatistics(List<DeviceRecord> deviceRecords) {
        List<DeviceStatistics> result = new ArrayList<>();
        for (DeviceRecord device : deviceRecords) {
            result.add(new DeviceStatistics(device.getDeviceId(), device.getDeviceName(), device.getFloorName(),
                    BigDecimal.ZERO, 0L, formatDuration(0L), Collections.emptyList()));
        }
        return result;
    }

    private String boundaryKey(int index) {
        return "b" + index;
    }

    private void validateYear(int year) {
        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException("年份必须在 1 到 9999 之间");
        }
    }

    private int lastMonthToReturn(int year, LocalDateTime now) {
        if (year < now.getYear()) {
            return 12;
        }
        if (year == now.getYear()) {
            return now.getMonthValue();
        }
        return 0;
    }

    private static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    public static class AnnualStatistics {
        private final int year;
        private final LocalDateTime generatedAt;
        private final List<MonthHeader> months;
        private final List<DeviceStatistics> devices;

        public AnnualStatistics(int year, LocalDateTime generatedAt,
                                List<MonthHeader> months, List<DeviceStatistics> devices) {
            this.year = year;
            this.generatedAt = generatedAt;
            this.months = months;
            this.devices = devices;
        }

        public int getYear() { return year; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public List<MonthHeader> getMonths() { return months; }
        public List<DeviceStatistics> getDevices() { return devices; }
    }

    public static class MonthHeader {
        private final int month;
        private final String label;

        public MonthHeader(int month, String label) {
            this.month = month;
            this.label = label;
        }

        public int getMonth() { return month; }
        public String getLabel() { return label; }
    }

    public static class DeviceStatistics {
        private final String deviceId;
        private final String deviceName;
        private final String floorName;
        private final BigDecimal totalDifferenceKwh;
        private final long totalOnlineSeconds;
        private final String totalOnlineDuration;
        private final List<MonthlyStatistic> monthlyStatistics;

        public DeviceStatistics(String deviceId, String deviceName, String floorName,
                                BigDecimal totalDifferenceKwh, long totalOnlineSeconds,
                                String totalOnlineDuration, List<MonthlyStatistic> monthlyStatistics) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.floorName = floorName;
            this.totalDifferenceKwh = totalDifferenceKwh;
            this.totalOnlineSeconds = totalOnlineSeconds;
            this.totalOnlineDuration = totalOnlineDuration;
            this.monthlyStatistics = monthlyStatistics;
        }

        public String getDeviceId() { return deviceId; }
        public String getDeviceName() { return deviceName; }
        public String getFloorName() { return floorName; }
        public BigDecimal getTotalDifferenceKwh() { return totalDifferenceKwh; }
        public long getTotalOnlineSeconds() { return totalOnlineSeconds; }
        public String getTotalOnlineDuration() { return totalOnlineDuration; }
        public List<MonthlyStatistic> getMonthlyStatistics() { return monthlyStatistics; }
    }

    public static class MonthlyStatistic {
        private final int month;
        private final boolean hasData;
        private final BigDecimal differenceKwh;
        private final Long totalOnlineSeconds;
        private final String totalOnlineDuration;

        private MonthlyStatistic(int month, boolean hasData, BigDecimal differenceKwh,
                                 Long totalOnlineSeconds, String totalOnlineDuration) {
            this.month = month;
            this.hasData = hasData;
            this.differenceKwh = differenceKwh;
            this.totalOnlineSeconds = totalOnlineSeconds;
            this.totalOnlineDuration = totalOnlineDuration;
        }

        public static MonthlyStatistic of(int month, BigDecimal differenceKwh, long onlineSeconds) {
            return new MonthlyStatistic(month, true, differenceKwh,
                    onlineSeconds, formatDuration(onlineSeconds));
        }

        public static MonthlyStatistic empty(int month) {
            return new MonthlyStatistic(month, false, null, null, null);
        }

        public int getMonth() { return month; }
        public boolean isHasData() { return hasData; }
        public BigDecimal getDifferenceKwh() { return differenceKwh; }
        public Long getTotalOnlineSeconds() { return totalOnlineSeconds; }
        public String getTotalOnlineDuration() { return totalOnlineDuration; }
    }
}
