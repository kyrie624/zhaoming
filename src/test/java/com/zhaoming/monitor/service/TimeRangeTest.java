package com.zhaoming.monitor.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeRangeTest {
    @Test
    void acceptsIsoAndLocalDateTimeFormats() {
        TimeRange range = TimeRange.parse("2026-08-25T00:00:00+08:00", "2026-08-25 23:59:59");

        assertEquals("2026-08-25T00:00", range.getStart().toString());
        assertEquals("2026-08-25T23:59:59", range.getEnd().toString());
    }
}
