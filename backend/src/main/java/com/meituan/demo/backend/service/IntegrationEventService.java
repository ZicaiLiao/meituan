package com.meituan.demo.backend.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IntegrationEventService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationEventService.class);

    public void publish(String topic, Map<String, Object> payload) {
        log.info("Publishing topic={} payload={}", topic, payload);
    }
}

