package com.zhaoming.monitor.mapper;

import com.zhaoming.monitor.model.MeasurementRecord;
import com.zhaoming.monitor.model.DeviceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MeasurementMapper {
    int upsert(MeasurementRecord record);

    MeasurementRecord findLatestBefore(@Param("deviceId") String deviceId,
                                       @Param("time") LocalDateTime time);

    MeasurementRecord findEarliestAfter(@Param("deviceId") String deviceId,
                                       @Param("time") LocalDateTime time);

    List<DeviceRecord> findDevices(@Param("deviceName") String deviceName,
                                   @Param("floorName") String floorName);
}
