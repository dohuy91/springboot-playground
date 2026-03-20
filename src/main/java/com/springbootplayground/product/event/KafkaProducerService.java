package com.springbootplayground.product.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final String PRODUCTS_TOPIC = "products";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendProductCreatedEvent(ProductCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(PRODUCTS_TOPIC, event.sku(), payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send ProductCreatedEvent to Kafka", e);
        }
    }
}
