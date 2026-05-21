package com.meituan.demo.backend.service;

import java.util.Map;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class IntegrationEventService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationEventService.class);

    private final RocketMQTemplate rocketMQTemplate;

    public IntegrationEventService(ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider) {
        this.rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
    }

    public void publish(String topic, Map<String, Object> payload) {
        if (rocketMQTemplate == null) {
            log.info("RocketMQ template unavailable, skipping publish for topic={} payload={}", topic, payload);
            return;
        }
        try {
            rocketMQTemplate.convertAndSend(topic, payload);
        } catch (Exception ex) {
            log.warn("RocketMQ publish failed, topic={} payload={}", topic, payload, ex);
        }
    }
}
