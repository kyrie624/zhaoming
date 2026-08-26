package com.zhaoming.monitor.service;

import com.zhaoming.monitor.mapper.WorkPeriodMapper;
import com.zhaoming.monitor.model.WorkPeriodRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkPeriodService {
    private final WorkPeriodMapper mapper;

    public WorkPeriodService(WorkPeriodMapper mapper) {
        this.mapper = mapper;
    }

    public List<WorkPeriod> find(String deviceId, TimeRange range) {
        List<WorkPeriodRecord> records = mapper.findOverlapping(deviceId, range.getStart(), range.getEnd());
        List<WorkPeriod> result = new ArrayList<>();
        for (WorkPeriodRecord record : records) {
            LocalDateTime start = record.getStartAt().isBefore(range.getStart())
                    ? range.getStart() : record.getStartAt();
            LocalDateTime end = record.getEndAt() == null ? null
                    : (record.getEndAt().isAfter(range.getEnd()) ? range.getEnd() : record.getEndAt());
            result.add(new WorkPeriod(deviceId, record.getDeviceName(), record.getFloorName(),
                    start, end, record.getEndAt() == null));
        }
        return result;
    }

    /** 按电表名称、区域和时间范围筛选工作区间。 */
    public List<WorkPeriod> findByFilter(String deviceName, String floorName, TimeRange range) {
        List<WorkPeriodRecord> records = mapper.findOverlappingByFilter(
                deviceName, floorName, range.getStart(), range.getEnd());
        return toWorkPeriods(records, range);
    }

    private List<WorkPeriod> toWorkPeriods(List<WorkPeriodRecord> records, TimeRange range) {
        List<WorkPeriod> result = new ArrayList<>();
        for (WorkPeriodRecord record : records) {
            LocalDateTime start = record.getStartAt().isBefore(range.getStart())
                    ? range.getStart() : record.getStartAt();
            LocalDateTime end = record.getEndAt() == null ? null
                    : (record.getEndAt().isAfter(range.getEnd()) ? range.getEnd() : record.getEndAt());
            result.add(new WorkPeriod(record.getDeviceId(), record.getDeviceName(), record.getFloorName(),
                    start, end, record.getEndAt() == null));
        }
        return result;
    }

    public static class WorkPeriod {
        private final String deviceId;
        private final String deviceName;
        private final String floorName;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final boolean ongoing;

        public WorkPeriod(String deviceId, String deviceName, String floorName,
                          LocalDateTime startTime, LocalDateTime endTime, boolean ongoing) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.floorName = floorName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.ongoing = ongoing;
        }

        public String getDeviceId() { return deviceId; }
        public String getDeviceName() { return deviceName; }
        public String getFloorName() { return floorName; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public boolean isOngoing() { return ongoing; }
    }
}
