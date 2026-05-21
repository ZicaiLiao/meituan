package com.meituan.demo.backend.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        Runnable publisher = () -> {
            try {
                rocketMQTemplate.convertAndSend(topic, payload);
            } catch (Exception ex) {
                log.warn("RocketMQ publish failed, topic={} payload={}", topic, payload, ex);
            }
        };

        // Publish after commit so MQ latency or retries do not block the checkout flow, while still
        // keeping emitted events aligned with committed business state.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(publisher);
                }
            });
            return;
        }

        CompletableFuture.runAsync(publisher);
    }
}
