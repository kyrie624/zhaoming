package com.zhaoming.monitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhaoming.monitor.model.MeterMeasurement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeterResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeterResponseParser parser = new MeterResponseParser();

    @Test
    void parsesAttachmentShapeAndUsesAnyPhaseAbovePointFive() throws Exception {
        ObjectNode response = (ObjectNode) objectMapper.readTree(
                "{\"success\":true,\"data\":{\"list\":[{"
                        + "\"deviceId\":\"d172\",\"name\":\"测试电表\",\"floorName\":\"混匀区\","
                        + "\"lastTime\":\"2026-08-25T21:59:00+08:00\","
                        + "\"properties\":["
                        + "{\"name\":\"有功电能\",\"currentValue\":\"54730.30\"},"
                        + "{\"name\":\"A相电流\",\"currentValue\":\"0.50\"},"
                        + "{\"name\":\"B相电流\",\"currentValue\":\"0.51\"},"
                        + "{\"name\":\"C相电流\",\"currentValue\":\"0.00\"}]}]}}" );

        List<MeterMeasurement> measurements = parser.parse(response);

        assertEquals(1, measurements.size());
        MeterMeasurement measurement = measurements.get(0);
        assertEquals("d172", measurement.getDeviceId());
        assertEquals("测试电表", measurement.getDeviceName());
        assertEquals("混匀区", measurement.getFloorName());
        assertEquals(new BigDecimal("54730.30"), measurement.getEnergyKwh());
        assertEquals(new BigDecimal("0.51"), measurement.getCurrentB());
        assertTrue(measurement.isWorking());
    }

    @Test
    void thresholdIsStrictlyGreaterThanPointFive() throws Exception {
        ObjectNode response = (ObjectNode) objectMapper.readTree(
                "{\"success\":true,\"data\":{\"list\":[{\"deviceId\":\"d1\","
                        + "\"properties\":[{\"name\":\"A相电流\",\"currentValue\":\"0.5\"}]}]}}" );

        List<MeterMeasurement> measurements = parser.parse(response);

        assertFalse(measurements.get(0).isWorking());
    }
}
