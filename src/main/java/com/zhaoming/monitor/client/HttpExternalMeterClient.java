package com.zhaoming.monitor.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 不带鉴权的外部 HTTP 接口实现。URL 为空时采集任务保持停用。 */
@Component
public class HttpExternalMeterClient implements ExternalMeterClient {

    private static final Logger log = LoggerFactory.getLogger(HttpExternalMeterClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String url;
    private final List<String> deviceIds;

    public HttpExternalMeterClient(RestTemplateBuilder builder,
                                   @Value("${meter.external.url:}") String url,
                                   @Value("${meter.external.device-ids:}") String deviceIds,
                                   ObjectMapper objectMapper,
                                   @Value("${meter.external.connect-timeout-ms:5000}") int connectTimeout,
                                   @Value("${meter.external.read-timeout-ms:10000}") int readTimeout) {
        this.url = url;
        this.objectMapper = objectMapper;
        this.deviceIds = parseDeviceIds(deviceIds);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = builder.requestFactory(() -> factory).build();
    }

    @Override
    public JsonNode fetchMeters() {
        if (url == null || url.trim().isEmpty()) {
            log.debug("meter.external.url 未配置，跳过本次设备采集");
            return null;
        }
        // 外部接口要求 deviceId 逐个传入；这里将每个设备的响应合并成统一的 data.list 结构。
        if (deviceIds.isEmpty()) {
            return restTemplate.getForObject(url, JsonNode.class);
        }

        ArrayNode devices = objectMapper.createArrayNode();
        boolean hasSuccessfulResponse = false;
        for (String deviceId : deviceIds) {
            try {
                String requestUrl = UriComponentsBuilder.fromHttpUrl(url)
                        .replaceQueryParam("deviceId", deviceId)
                        .build()
                        .encode()
                        .toUriString();
                JsonNode response = restTemplate.getForObject(requestUrl, JsonNode.class);
                if (response != null && response.path("success").asBoolean(false)
                        && response.path("data").path("list").isArray()) {
                    hasSuccessfulResponse = true;
                    response.path("data").path("list").forEach(devices::add);
                } else {
                    log.warn("设备 {} 接口返回失败或数据为空", deviceId);
                }
            } catch (Exception e) {
                // 单个设备失败不影响同一轮其他设备采集。
                log.error("设备 {} 采集失败", deviceId, e);
            }
        }

        ObjectNode combined = objectMapper.createObjectNode();
        combined.put("success", hasSuccessfulResponse);
        combined.put("code", hasSuccessfulResponse ? 200 : 500);
        combined.put("msg", hasSuccessfulResponse ? "success" : "all device requests failed");
        ObjectNode data = combined.putObject("data");
        data.set("list", devices);
        data.put("total", devices.size());
        return combined;
    }

    private List<String> parseDeviceIds(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .forEach(result::add);
        return result;
    }
}
