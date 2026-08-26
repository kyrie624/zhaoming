package com.zhaoming.monitor.service;

import com.zhaoming.monitor.mapper.MeasurementMapper;
import com.zhaoming.monitor.mapper.WorkPeriodMapper;
import com.zhaoming.monitor.model.MeasurementRecord;
import com.zhaoming.monitor.model.MeterMeasurement;
import com.zhaoming.monitor.model.WorkPeriodRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MeasurementService {

    private final MeasurementMapper measurementMapper;
    private final WorkPeriodMapper workPeriodMapper;

    public MeasurementService(MeasurementMapper measurementMapper, WorkPeriodMapper workPeriodMapper) {
        this.measurementMapper = measurementMapper;
        this.workPeriodMapper = workPeriodMapper;
    }

    /** 保存采样，并在状态边沿发生时创建或关闭工作区间。 */
    @Transactional
    public void record(MeterMeasurement measurement) {
        MeasurementRecord record = new MeasurementRecord();
        record.setDeviceId(measurement.getDeviceId());
        record.setDeviceName(measurement.getDeviceName());
        record.setFloorName(measurement.getFloorName());
        record.setCollectedAt(measurement.getCollectedAt());
        record.setEnergyKwh(measurement.getEnergyKwh());
        record.setCurrentA(measurement.getCurrentA());
        record.setCurrentB(measurement.getCurrentB());
        record.setCurrentC(measurement.getCurrentC());
        record.setWorking(measurement.isWorking());
        measurementMapper.upsert(record);

        WorkPeriodRecord open = workPeriodMapper.findOpen(measurement.getDeviceId());
        if (measurement.isWorking() && open == null) {
            WorkPeriodRecord period = new WorkPeriodRecord();
            period.setDeviceId(measurement.getDeviceId());
            period.setDeviceName(measurement.getDeviceName());
            period.setFloorName(measurement.getFloorName());
            period.setStartAt(measurement.getCollectedAt());
            workPeriodMapper.insert(period);
        } else if (!measurement.isWorking() && open != null
                && !measurement.getCollectedAt().isBefore(open.getStartAt())) {
            workPeriodMapper.close(open.getId(), measurement.getCollectedAt());
        }
    }
}
