package com.springbootplayground.product.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductCreatedEvent(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String sku,
        Integer quantity,
        Boolean active,
        Instant createdAt
) {
}
