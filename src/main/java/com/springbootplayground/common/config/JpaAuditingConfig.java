package com.springbootplayground.common.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.springbootplayground.common.clock.ClockProvider;

/**
 * Enables Spring Data JPA auditing and wires {@link ClockProvider}
 * as the single authoritative time source for {@code @CreatedDate}
 * and {@code @LastModifiedDate} fields.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditDateTimeProvider(ClockProvider clockProvider) {
        return () -> Optional.of(clockProvider.now());
    }
}

