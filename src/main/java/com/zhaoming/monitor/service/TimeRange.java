package com.zhaoming.monitor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimeRange {
    private static final DateTimeFormatter LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime start;
    private final LocalDateTime end;

    private TimeRange(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public static TimeRange parse(String start, String end) {
        LocalDateTime startTime = parseTime(start);
        LocalDateTime endTime = parseTime(end);
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        return new TimeRange(startTime, endTime);
    }

    private static LocalDateTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        String normalized = value.trim();
        try {
            return OffsetDateTime.parse(normalized).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ignoredAgain) {
                try {
                    // 兼容 yyyy-MM-dd HH:mm:ss[.SSS] 格式。
                    return LocalDateTime.parse(normalized.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (DateTimeParseException localWithSpace) {
                    try {
                        return LocalDateTime.parse(normalized, LOCAL_FORMAT);
                    } catch (DateTimeParseException invalidTime) {
                        try {
                            return LocalDate.parse(normalized).atStartOfDay();
                        } catch (DateTimeParseException invalidDate) {
                            throw new IllegalArgumentException("时间格式不正确，支持 yyyy-MM-dd HH:mm:ss 或 ISO-8601", invalidDate);
                        }
                    }
                }
            }
        }
    }

    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
}
