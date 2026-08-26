package com.zhaoming.monitor.mapper;

import com.zhaoming.monitor.model.WorkPeriodRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorkPeriodMapper {
    WorkPeriodRecord findOpen(@Param("deviceId") String deviceId);

    int insert(WorkPeriodRecord period);

    int close(@Param("id") Long id, @Param("endAt") LocalDateTime endAt);

    List<WorkPeriodRecord> findOverlapping(@Param("deviceId") String deviceId,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endAt") LocalDateTime endAt);

    List<WorkPeriodRecord> findOverlappingByFilter(@Param("deviceName") String deviceName,
                                                   @Param("floorName") String floorName,
                                                   @Param("startAt") LocalDateTime startAt,
                                                   @Param("endAt") LocalDateTime endAt);
}
