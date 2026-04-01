package com.springbootplayground.product.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbootplayground.messaging.service.KafkaMessageRecordService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

    private final ObjectMapper objectMapper;
    private final KafkaMessageRecordService kafkaMessageRecordService;

    @KafkaListener(topics = "products", groupId = "${spring.kafka.consumer.group-id}")
    public void handle(ConsumerRecord<String, String> record) {
        try {
            ProductCreatedEvent event = objectMapper.readValue(record.value(), ProductCreatedEvent.class);
            log.info("Received ProductCreatedEvent: sku={}, productId={}", event.sku(), event.id());
            // Business processing of the consumed event goes here.
            kafkaMessageRecordService.recordConsumerSuccess(record);
        } catch (Exception ex) {
            // Re-throw so the DefaultErrorHandler retries and eventually persists the failure.
            throw new RuntimeException("Failed to process ProductCreatedEvent from topic=" + record.topic(), ex);
        }
    }
}
