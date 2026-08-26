package com.zhaoming.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoming.monitor.client.ExternalMeterClient;
import com.zhaoming.monitor.model.MeterMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MeasurementCollector {

    private static final Logger log = LoggerFactory.getLogger(MeasurementCollector.class);

    private final ExternalMeterClient client;
    private final MeterResponseParser parser;
    private final MeasurementService measurementService;

    public MeasurementCollector(ExternalMeterClient client, MeterResponseParser parser,
                                MeasurementService measurementService) {
        this.client = client;
        this.parser = parser;
        this.measurementService = measurementService;
    }

    @Scheduled(fixedRateString = "${meter.collection.fixed-rate-ms:60000}",
            initialDelayString = "${meter.collection.initial-delay-ms:0}")
    public void collect() {
        try {
            JsonNode response = client.fetchMeters();
            List<MeterMeasurement> measurements = parser.parse(response);
            for (MeterMeasurement measurement : measurements) {
                measurementService.record(measurement);
            }
            if (!measurements.isEmpty()) {
                log.info("设备采集完成，保存 {} 条设备采样", measurements.size());
            }
        } catch (Exception e) {
            // 单次外部接口失败不应阻止下一分钟的任务。
            log.error("设备采集失败", e);
        }
    }
}
