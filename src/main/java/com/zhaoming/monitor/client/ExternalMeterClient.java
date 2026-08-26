package com.zhaoming.monitor.client;

import com.fasterxml.jackson.databind.JsonNode;

/** 外部接口适配层。以后更换接口地址、鉴权方式或协议时只需替换此实现。 */
public interface ExternalMeterClient {
    JsonNode fetchMeters();
}
