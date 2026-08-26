package com.zhaoming.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoming.monitor.model.MeterMeasurement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 解析附件所示 data.list[].properties[] 结构。 */
@Component
public class MeterResponseParser {

    private static final DateTimeFormatter LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public List<MeterMeasurement> parse(JsonNode response) {
        if (response == null || !response.path("success").asBoolean(false)) {
            return Collections.emptyList();
        }
        JsonNode devices = response.path("data").path("list");
        if (!devices.isArray()) {
            return Collections.emptyList();
        }

        List<MeterMeasurement> result = new ArrayList<>();
        for (JsonNode device : devices) {
            String deviceId = text(device, "deviceId");
            if (deviceId == null || deviceId.trim().isEmpty()) {
                continue;
            }
            JsonNode properties = device.path("properties");
            BigDecimal energy = null;
            BigDecimal currentA = null;
            BigDecimal currentB = null;
            BigDecimal currentC = null;
            LocalDateTime collectedAt = parseTime(text(device, "lastTime"));
            if (properties.isArray()) {
                for (JsonNode property : properties) {
                    String name = text(property, "name");
                    BigDecimal value = decimal(property, "currentValue");
                    if ("有功电能".equals(name)) {
                        energy = value;
                    } else if ("A相电流".equals(name)) {
                        currentA = value;
                    } else if ("B相电流".equals(name)) {
                        currentB = value;
                    } else if ("C相电流".equals(name)) {
                        currentC = value;
                    }
                    if (collectedAt == null) {
                        collectedAt = parseTime(text(property, "lastTime"));
                    }
                }
            }
            if (collectedAt == null) {
                collectedAt = LocalDateTime.now();
            }
            if (energy == null && currentA == null && currentB == null && currentC == null) {
                continue;
            }
            boolean working = greaterThanThreshold(currentA)
                    || greaterThanThreshold(currentB)
                    || greaterThanThreshold(currentC);
            result.add(new MeterMeasurement(deviceId, text(device, "name"), text(device, "floorName"), collectedAt,
                    energy, currentA, currentB, currentC, working));
        }
        return result;
    }

    private boolean greaterThanThreshold(BigDecimal value) {
        return value != null && value.compareTo(new BigDecimal("0.5")) > 0;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim();
        try {
            return OffsetDateTime.parse(normalized).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(normalized, LOCAL_FORMAT);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

}
