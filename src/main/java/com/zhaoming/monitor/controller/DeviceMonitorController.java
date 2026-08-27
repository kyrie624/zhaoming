package com.zhaoming.monitor.controller;

import com.zhaoming.monitor.service.AnnualMonthlyStatisticsService;
import com.zhaoming.monitor.service.EnergyService;
import com.zhaoming.monitor.service.TimeRange;
import com.zhaoming.monitor.service.WorkPeriodService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceMonitorController {
    private final EnergyService energyService;
    private final WorkPeriodService workPeriodService;
    private final AnnualMonthlyStatisticsService annualMonthlyStatisticsService;

    public DeviceMonitorController(EnergyService energyService, WorkPeriodService workPeriodService,
                                   AnnualMonthlyStatisticsService annualMonthlyStatisticsService) {
        this.energyService = energyService;
        this.workPeriodService = workPeriodService;
        this.annualMonthlyStatisticsService = annualMonthlyStatisticsService;
    }

    @GetMapping("/{deviceId}/work-periods")
    public List<WorkPeriodService.WorkPeriod> workPeriods(
            @PathVariable String deviceId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return workPeriodService.find(deviceId, parseRange(startTime, endTime));
    }

    @GetMapping("/work-periods")
    public List<WorkPeriodService.WorkPeriod> workPeriodsByFilter(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String floorName,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return workPeriodService.findByFilter(deviceName, floorName, parseRange(startTime, endTime));
    }

    @GetMapping("/{deviceId}/energy")
    public EnergyService.EnergyResult energy(
            @PathVariable String deviceId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return energyService.calculate(deviceId, parseRange(startTime, endTime));
    }

    @GetMapping("/energy")
    public List<EnergyService.EnergyResult> energyByFilter(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String floorName,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return energyService.calculateByFilter(deviceName, floorName, parseRange(startTime, endTime));
    }

    @GetMapping("/monthly-statistics")
    public AnnualMonthlyStatisticsService.AnnualStatistics monthlyStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String floorName) {
        try {
            return annualMonthlyStatisticsService.calculate(year, deviceName, floorName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private TimeRange parseRange(String startTime, String endTime) {
        try {
            return TimeRange.parse(startTime, endTime);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
