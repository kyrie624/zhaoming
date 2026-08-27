package com.zhaoming.monitor.service;

import com.zhaoming.monitor.mapper.MeasurementMapper;
import com.zhaoming.monitor.mapper.WorkPeriodMapper;
import com.zhaoming.monitor.model.DeviceRecord;
import com.zhaoming.monitor.model.EnergyBoundaryRecord;
import com.zhaoming.monitor.model.WorkPeriodRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnnualMonthlyStatisticsServiceTest {

    @Test
    void aggregatesThroughCurrentMonthWithOneBatchQueryPerDataType() {
        MeasurementMapper measurementMapper = mock(MeasurementMapper.class);
        WorkPeriodMapper workPeriodMapper = mock(WorkPeriodMapper.class);
        AnnualMonthlyStatisticsService service =
                new AnnualMonthlyStatisticsService(measurementMapper, workPeriodMapper);

        DeviceRecord device = device("d1", "电表001", "一层");
        when(measurementMapper.findDevices(null, null)).thenReturn(Collections.singletonList(device));
        when(measurementMapper.findEnergyBoundaries(anyList(), anyList()))
                .thenAnswer(invocation -> boundariesFor(invocation.getArgument(1),
                        Arrays.asList("0.00", "1.50", "4.00", "7.50")));
        when(workPeriodMapper.findOverlappingForDevices(anyList(), any(), any())).thenReturn(Arrays.asList(
                period("d1", LocalDateTime.of(2026, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 1, 1, 0, 1)),
                period("d1", LocalDateTime.of(2026, 2, 1, 0, 0),
                        LocalDateTime.of(2026, 2, 1, 0, 2)),
                period("d1", LocalDateTime.of(2026, 3, 1, 0, 0),
                        LocalDateTime.of(2026, 3, 1, 0, 3))));

        AnnualMonthlyStatisticsService.AnnualStatistics result =
                service.calculate(2026, null, null, LocalDateTime.of(2026, 3, 15, 12, 0));

        assertEquals(3, result.getMonths().size());
        AnnualMonthlyStatisticsService.DeviceStatistics row = result.getDevices().get(0);
        assertEquals(3, row.getMonthlyStatistics().size());
        assertTrue(row.getMonthlyStatistics().get(0).isHasData());
        assertEquals(new BigDecimal("1.50"), row.getMonthlyStatistics().get(0).getDifferenceKwh());
        assertEquals(new BigDecimal("7.50"), row.getTotalDifferenceKwh());
        assertEquals(360L, row.getTotalOnlineSeconds());
        assertEquals("00:06:00", row.getTotalOnlineDuration());
        verify(measurementMapper, times(1)).findEnergyBoundaries(anyList(), anyList());
        verify(workPeriodMapper, times(1)).findOverlappingForDevices(anyList(), any(), any());
    }

    @Test
    void returnsAllTwelveMonthsForPastYear() {
        MeasurementMapper measurementMapper = mock(MeasurementMapper.class);
        WorkPeriodMapper workPeriodMapper = mock(WorkPeriodMapper.class);
        AnnualMonthlyStatisticsService service =
                new AnnualMonthlyStatisticsService(measurementMapper, workPeriodMapper);
        when(measurementMapper.findDevices(null, null)).thenReturn(Collections.emptyList());

        AnnualMonthlyStatisticsService.AnnualStatistics result =
                service.calculate(2025, null, null, LocalDateTime.of(2026, 8, 27, 10, 30));

        assertEquals(12, result.getMonths().size());
        verify(measurementMapper, never()).findEnergyBoundaries(anyList(), anyList());
        verify(workPeriodMapper, never()).findOverlappingForDevices(anyList(), any(), any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void currentMonthBatchBoundaryAndPeriodRangeEndAtGenerationTime() {
        MeasurementMapper measurementMapper = mock(MeasurementMapper.class);
        WorkPeriodMapper workPeriodMapper = mock(WorkPeriodMapper.class);
        AnnualMonthlyStatisticsService service =
                new AnnualMonthlyStatisticsService(measurementMapper, workPeriodMapper);

        when(measurementMapper.findDevices(null, null))
                .thenReturn(Collections.singletonList(device("d1", "电表001", "一层")));
        when(measurementMapper.findEnergyBoundaries(anyList(), anyList())).thenReturn(Collections.emptyList());
        when(workPeriodMapper.findOverlappingForDevices(anyList(), any(), any()))
                .thenReturn(Collections.emptyList());
        LocalDateTime now = LocalDateTime.of(2026, 8, 27, 10, 30);

        service.calculate(2026, null, null, now);

        ArgumentCaptor<List> boundariesCaptor = ArgumentCaptor.forClass(List.class);
        verify(measurementMapper).findEnergyBoundaries(anyList(), boundariesCaptor.capture());
        List<EnergyBoundaryRecord> points = boundariesCaptor.getValue();
        assertEquals(9, points.size());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), points.get(0).getBoundaryAt());
        assertEquals(now, points.get(8).getBoundaryAt());

        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(workPeriodMapper).findOverlappingForDevices(
                anyList(), any(LocalDateTime.class), endCaptor.capture());
        assertEquals(now, endCaptor.getValue());
    }

    private DeviceRecord device(String id, String name, String floor) {
        DeviceRecord result = new DeviceRecord();
        result.setDeviceId(id);
        result.setDeviceName(name);
        result.setFloorName(floor);
        return result;
    }

    private List<EnergyBoundaryRecord> boundariesFor(List<EnergyBoundaryRecord> points,
                                                      List<String> energyValues) {
        List<EnergyBoundaryRecord> result = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            EnergyBoundaryRecord point = points.get(index);
            EnergyBoundaryRecord record = new EnergyBoundaryRecord();
            record.setDeviceId("d1");
            record.setBoundaryKey(point.getBoundaryKey());
            record.setBoundaryAt(point.getBoundaryAt());
            record.setCollectedAt(point.getBoundaryAt());
            record.setEnergyKwh(new BigDecimal(energyValues.get(index)));
            result.add(record);
        }
        return result;
    }

    private WorkPeriodRecord period(String deviceId, LocalDateTime start, LocalDateTime end) {
        WorkPeriodRecord result = new WorkPeriodRecord();
        result.setDeviceId(deviceId);
        result.setStartAt(start);
        result.setEndAt(end);
        return result;
    }
}
