package com.springbootplayground.product.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbootplayground.product.service.KafkaMessageRecordService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    private static final String PRODUCTS_TOPIC = "products";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaMessageRecordService kafkaMessageRecordService;

    public void sendProductCreatedEvent(ProductCreatedEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ProductCreatedEvent for sku={}", event.sku(), e);
            kafkaMessageRecordService.recordProducerFailed(PRODUCTS_TOPIC, event.sku(), event.toString(), e);
            return;
        }

        kafkaTemplate.send(PRODUCTS_TOPIC, event.sku(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ProductCreatedEvent to Kafka for sku={}", event.sku(), ex);
                        kafkaMessageRecordService.recordProducerFailed(
                                PRODUCTS_TOPIC, event.sku(), payload, (Exception) ex);
                    } else {
                        log.debug("ProductCreatedEvent published successfully for sku={}", event.sku());
                        kafkaMessageRecordService.recordProducerSent(PRODUCTS_TOPIC, event.sku(), payload);
                    }
                });
    }
}
